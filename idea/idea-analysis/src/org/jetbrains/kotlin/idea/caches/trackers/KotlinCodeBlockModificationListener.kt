/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.trackers

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.PomManager
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.pom.tree.events.impl.ChangeInfoImpl
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.CHILD_MOVED
import com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.PROPERTY_CHANGED
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

val KOTLIN_CONSOLE_KEY = Key.create<Boolean>("kotlin.console")

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
class KotlinCodeBlockModificationListener(
    modificationTracker: PsiModificationTracker,
    project: Project,
    private val treeAspect: TreeAspect
) : PsiTreeChangePreprocessor {
    private val modificationTrackerImpl = modificationTracker as PsiModificationTrackerImpl

    @Suppress("UnstableApiUsage")
    private val isLanguageTrackerEnabled = modificationTrackerImpl.isEnableLanguageTrackerCompat

    // BUNCH: 191
    // When there're we no per-language trackers we had to increment global tracker first and process result afterward
    private val customIncrement = if (isLanguageTrackerEnabled) 0 else 1

    @Volatile
    private var kotlinModificationTracker: Long = 0

    private val kotlinOutOfCodeBlockTrackerImpl: SimpleModificationTracker = if (isLanguageTrackerEnabled) {
        SimpleModificationTracker()
    } else {
        object : SimpleModificationTracker() {
            override fun getModificationCount(): Long {
                @Suppress("DEPRECATION")
                return modificationTracker.outOfCodeBlockModificationCount
            }
        }
    }

    val kotlinOutOfCodeBlockTracker: ModificationTracker = kotlinOutOfCodeBlockTrackerImpl

    internal val perModuleOutOfCodeBlockTrackerUpdater = KotlinModuleOutOfCodeBlockModificationTracker.Updater(project)

    init {
        val model = PomManager.getModel(project)
        val messageBusConnection = project.messageBus.connect()

        if (isLanguageTrackerEnabled) {
            (PsiManager.getInstance(project) as PsiManagerImpl).addTreeChangePreprocessor(this)
        }

        model.addModelListener(object : PomModelListener {
            override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean {
                return aspect == treeAspect
            }

            override fun modelChanged(event: PomModelEvent) {
                val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return
                val ktFile = changeSet.rootElement.psi.containingFile as? KtFile ?: return

                incFileModificationCount(ktFile)

                val changedElements = changeSet.changedElements

                // skip change if it contains only virtual/fake change
                if (changedElements.isNotEmpty() &&
                    // ignore formatting (whitespaces etc) change
                    (isFormattingChange(changeSet) ||
                            // ignore comment change
                            isCommentChange(changeSet) ||
                            changedElements.all { !it.psi.isPhysical })
                ) return

                val inBlockChange = inBlockModifications(changedElements)

                if (!inBlockChange) {
                    messageBusConnection.deliverImmediately()

                    if (ktFile.isPhysical && !isReplLine(ktFile.virtualFile)) {
                        if (isLanguageTrackerEnabled) {
                            kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                            perModuleOutOfCodeBlockTrackerUpdater.onKotlinPhysicalFileOutOfBlockChange(ktFile, true)
                        } else {
                            perModuleOutOfCodeBlockTrackerUpdater.onKotlinPhysicalFileOutOfBlockChange(ktFile, false)
                            // Increment counter and process changes in PsiModificationTracker.Listener
                            modificationTrackerImpl.incCounter()
                        }
                    }

                    incOutOfBlockModificationCount(ktFile)
                }
            }
        })

        @Suppress("UnstableApiUsage")
        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            if (isLanguageTrackerEnabled) {
                val kotlinTrackerInternalIDECount =
                    modificationTrackerImpl.forLanguage(KotlinLanguage.INSTANCE).modificationCount
                if (kotlinModificationTracker == kotlinTrackerInternalIDECount) {
                    // Some update that we are not sure is from Kotlin language, as Kotlin language tracker wasn't changed
                    kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
                } else {
                    kotlinModificationTracker = kotlinTrackerInternalIDECount
                }
            }

            perModuleOutOfCodeBlockTrackerUpdater.onPsiModificationTrackerUpdate(customIncrement)
        })
    }

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        assert(isLanguageTrackerEnabled)

        if (!PsiModificationTrackerImpl.canAffectPsi(event)) {
            return
        }

        // Copy logic from PsiModificationTrackerImpl.treeChanged(). Some out-of-code-block events are written to language modification
        // tracker in PsiModificationTrackerImpl but don't have correspondent PomModelEvent. Increase kotlinOutOfCodeBlockTracker
        // manually if needed.
        val outOfCodeBlock = when (event.code) {
            PROPERTY_CHANGED ->
                event.propertyName === PsiTreeChangeEvent.PROP_UNLOADED_PSI || event.propertyName === PsiTreeChangeEvent.PROP_ROOTS
            CHILD_MOVED -> event.oldParent is PsiDirectory || event.newParent is PsiDirectory
            else -> event.parent is PsiDirectory
        }

        if (outOfCodeBlock) {
            kotlinOutOfCodeBlockTrackerImpl.incModificationCount()
        }
    }

    companion object {
        private fun isReplLine(file: VirtualFile): Boolean {
            return file.getUserData(KOTLIN_CONSOLE_KEY) == true
        }

        private fun incOutOfBlockModificationCount(file: KtFile) {
            file.clearInBlockModifications()

            val count = file.getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0
            file.putUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, count + 1)
        }

        private fun incFileModificationCount(file: KtFile) {
            val tracker = file.getUserData(PER_FILE_MODIFICATION_TRACKER)
                ?: file.putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())
            tracker.incModificationCount()
        }

        private fun inBlockModifications(elements: Array<ASTNode>): Boolean {
            // When a code fragment is reparsed, Intellij doesn't do an AST diff and considers the entire
            // contents to be replaced, which is represented in a POM event as an empty list of changed elements
            if (elements.isEmpty()) return false

            val inBlockElements = mutableSetOf<KtElement>()
            for (element in elements) {
                // skip fake PSI elements like `IntellijIdeaRulezzz$`
                val psi = element.psi
                if (!psi.isPhysical) continue

                val modificationScope = getInsideCodeBlockModificationScope(psi) ?: return false

                inBlockElements.add(modificationScope.blockDeclaration)
            }

            inBlockElements.forEach { it.containingKtFile.addInBlockModifiedItem(it) }
            return inBlockElements.isNotEmpty()
        }

        private fun isCommentChange(changeSet: TreeChangeEvent): Boolean =
            changeSet.changedElements.all { changedElement ->
                val changesByElement = changeSet.getChangesByElement(changedElement)
                changesByElement.affectedChildren.all { affectedChild ->
                    if (!(affectedChild is PsiComment || affectedChild is KDoc)) return@all false
                    val changeByChild = changesByElement.getChangeByChild(affectedChild)
                    return@all if (changeByChild is ChangeInfoImpl) {
                        val oldChild = changeByChild.oldChild
                        oldChild is PsiComment || oldChild is KDoc
                    } else false
                }
            }

        private fun isFormattingChange(changeSet: TreeChangeEvent): Boolean =
            changeSet.changedElements.all {
                changeSet.getChangesByElement(it).affectedChildren.all { c -> c is PsiWhiteSpace }
            }

        /**
         * Has to be aligned with [getInsideCodeBlockModificationScope] :
         *
         * result of analysis has to be reflected in dirty scope,
         * the only difference is whitespaces and comments
         */
        fun getInsideCodeBlockModificationDirtyScope(element: PsiElement): PsiElement? {
            if (!element.isPhysical) return null
            // dirty scope for whitespaces and comments is the element itself
            if (element is PsiWhiteSpace || element is PsiComment || element is KDoc) return element

            return getInsideCodeBlockModificationScope(element)?.blockDeclaration ?: null
        }

        fun getInsideCodeBlockModificationScope(element: PsiElement): BlockModificationScopeElement? {
            val lambda = element.getTopmostParentOfType<KtLambdaExpression>()
            if (lambda is KtLambdaExpression) {
                lambda.getTopmostParentOfType<KtSuperTypeCallEntry>()?.let {
                    return BlockModificationScopeElement(it, it)
                }
            }

            val blockDeclaration =
                KtPsiUtil.getTopmostParentOfTypes(element, *BLOCK_DECLARATION_TYPES) as? KtDeclaration ?: return null

            // should not be local declaration
            if (KtPsiUtil.isLocal(blockDeclaration))
                return null

            when (blockDeclaration) {
                is KtNamedFunction -> {
                    if (blockDeclaration.hasBlockBody()) {
                        // case like `fun foo(): String {...<caret>...}`
                        return blockDeclaration.bodyExpression
                            ?.takeIf { it.isAncestor(element) }
                            ?.let { BlockModificationScopeElement(blockDeclaration, it) }
                    } else if (blockDeclaration.hasDeclaredReturnType()) {
                        // case like `fun foo(): String = b<caret>labla`
                        return blockDeclaration.initializer
                            ?.takeIf { it.isAncestor(element) }
                            ?.let { BlockModificationScopeElement(blockDeclaration, it) }
                    }
                }

                is KtProperty -> {
                    if (blockDeclaration.typeReference != null) {
                        val accessors =
                            blockDeclaration.accessors.map { it.initializer ?: it.bodyExpression } + blockDeclaration.initializer
                        for (accessor in accessors) {
                            accessor?.takeIf {
                                it.isAncestor(element) &&
                                        // adding annotations to accessor is the same as change contract of property
                                        (element !is KtAnnotated || element.annotationEntries.isEmpty())
                            }
                                ?.let { expression ->
                                    val declaration = if (blockDeclaration.initializer != null)
                                        blockDeclaration
                                    else
                                    // property could be initialized on a class level
                                        KtPsiUtil.getTopmostParentOfTypes(blockDeclaration, KtClass::class.java) as? KtElement ?:
                                        // ktFile to check top level property declarations
                                        return null
                                    return BlockModificationScopeElement(declaration, expression)
                                }
                        }
                    }
                }

                is KtScriptInitializer -> {
                    return (blockDeclaration.body as? KtCallExpression)
                        ?.lambdaArguments
                        ?.lastOrNull()
                        ?.getLambdaExpression()
                        ?.takeIf { it.isAncestor(element) }
                        ?.let { BlockModificationScopeElement(blockDeclaration, it) }
                }

                is KtClassInitializer -> {
                    blockDeclaration
                        .takeIf { it.isAncestor(element) }
                        ?.let { ktClassInitializer ->
                            (PsiTreeUtil.getParentOfType(blockDeclaration, KtClass::class.java) as? KtElement)?.let {
                                return BlockModificationScopeElement(it, ktClassInitializer)
                            }
                        }
                }

                is KtSecondaryConstructor -> {
                    blockDeclaration
                        ?.takeIf {
                            it.bodyExpression?.isAncestor(element) ?: false || it.getDelegationCallOrNull()?.isAncestor(element) ?: false
                        }
                        ?.let { ktConstructor ->
                            (PsiTreeUtil.getParentOfType(blockDeclaration, KtClass::class.java) as? KtElement)?.let {
                                return BlockModificationScopeElement(it, ktConstructor)
                            }
                        }
                }

                // TODO: still under consideration - is it worth to track changes of private properties / methods
                // problem could be in diagnostics - it is worth to manage it with modTracker
//                is KtClass -> {
//                    return when (element) {
//                        is KtProperty -> if (element.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) blockDeclaration else null
//                        is KtNamedFunction -> if (element.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) blockDeclaration else null
//                        else -> null
//                    }
//                }

                else -> throw IllegalStateException()
            }

            return null
        }

        data class BlockModificationScopeElement(val blockDeclaration: KtElement, val element: KtElement)

        fun isBlockDeclaration(declaration: KtDeclaration): Boolean {
            return BLOCK_DECLARATION_TYPES.any { it.isInstance(declaration) }
        }

        private val BLOCK_DECLARATION_TYPES = arrayOf<Class<out KtDeclaration>>(
            KtProperty::class.java,
            KtNamedFunction::class.java,
            KtClassInitializer::class.java,
            KtSecondaryConstructor::class.java,
            KtScriptInitializer::class.java
        )

        fun getInstance(project: Project): KotlinCodeBlockModificationListener =
            project.getComponent(KotlinCodeBlockModificationListener::class.java)
    }
}

private val PER_FILE_MODIFICATION_TRACKER = Key<SimpleModificationTracker>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.perFileModificationTracker: ModificationTracker
    get() = putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.outOfBlockModificationCount: Long by NotNullableUserDataProperty(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, 0)


/**
 * inBlockModifications is a collection of block elements those have in-block modifications
 */
private val IN_BLOCK_MODIFICATIONS = Key<MutableCollection<KtElement>>("IN_BLOCK_MODIFICATIONS")

val KtFile.inBlockModifications: Collection<KtElement>
    get() {
        val collection = getUserData(IN_BLOCK_MODIFICATIONS)
        return collection ?: emptySet()
    }

private fun KtFile.addInBlockModifiedItem(element: KtElement) {
    val collection = putUserDataIfAbsent(IN_BLOCK_MODIFICATIONS, mutableSetOf())
    synchronized(collection) {
        collection.add(element)
    }
}

fun KtFile.clearInBlockModifications() {
    val collection = getUserData(IN_BLOCK_MODIFICATIONS)
    collection?.let {
        synchronized(it) {
            it.clear()
        }
    }
}