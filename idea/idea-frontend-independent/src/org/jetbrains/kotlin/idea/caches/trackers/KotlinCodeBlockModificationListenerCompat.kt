/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

val KOTLIN_CONSOLE_KEY = Key.create<Boolean>("kotlin.console")

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
// FIX ME WHEN BUNCH 193 REMOVED
abstract class KotlinCodeBlockModificationListenerCompat(protected val project: Project) : PsiTreeChangePreprocessor {
    protected val modificationTrackerImpl: PsiModificationTrackerImpl =
        PsiModificationTracker.SERVICE.getInstance(project) as PsiModificationTrackerImpl

    @Volatile
    protected var kotlinModificationTracker: Long = 0

    protected lateinit var kotlinOutOfCodeBlockTrackerImpl: SimpleModificationTracker

    lateinit var kotlinOutOfCodeBlockTracker: ModificationTracker

    internal val perModuleOutOfCodeBlockTrackerUpdater = KotlinModuleOutOfCodeBlockModificationTracker.Updater(project)

    protected fun init(
        treeAspect: TreeAspect,
        incOCBCounter: (KtFile) -> Unit,
        psiModificationTrackerListener: PsiModificationTracker.Listener,
        kotlinOutOfCodeBlockTrackerProducer: () -> SimpleModificationTracker,
        isLanguageTrackerEnabled: Boolean = true,
    ) {
        kotlinOutOfCodeBlockTrackerImpl = kotlinOutOfCodeBlockTrackerProducer()
        kotlinOutOfCodeBlockTracker = kotlinOutOfCodeBlockTrackerImpl
        val model = PomManager.getModel(project)
        val messageBusConnection = project.messageBus.connect(project)
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
                if (changedElements.isNotEmpty()) {
                    // ignore formatting (whitespaces etc)
                    if (isFormattingChange(changeSet) || isCommentChange(changeSet)) return
                }

                val inBlockElements = inBlockModifications(changedElements)

                val physical = ktFile.isPhysical
                if (inBlockElements.isEmpty()) {
                    messageBusConnection.deliverImmediately()

                    if (physical && !isReplLine(ktFile.virtualFile) && ktFile !is KtTypeCodeFragment) {
                        incOCBCounter(ktFile)
                    }

                    ktFile.incOutOfBlockModificationCount()
                } else if (physical) {
                    inBlockElements.forEach { it.containingKtFile.addInBlockModifiedItem(it) }
                }
            }
        })

        if (isLanguageTrackerEnabled) {
            (PsiManager.getInstance(project) as PsiManagerImpl).addTreeChangePreprocessor(this)
        }
        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, psiModificationTrackerListener)
    }

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
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

        private fun incFileModificationCount(file: KtFile) {
            val tracker = file.getUserData(PER_FILE_MODIFICATION_TRACKER)
                ?: file.putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())
            tracker.incModificationCount()
        }

        private fun inBlockModifications(elements: Array<ASTNode>): List<KtElement> {
            // When a code fragment is reparsed, Intellij doesn't do an AST diff and considers the entire
            // contents to be replaced, which is represented in a POM event as an empty list of changed elements

            return elements.map { element ->
                val modificationScope = getInsideCodeBlockModificationScope(element.psi) ?: return emptyList()
                modificationScope.blockDeclaration
            }
        }

        private fun isSpecificChange(changeSet: TreeChangeEvent, precondition: (ASTNode?) -> Boolean): Boolean =
            changeSet.changedElements.all { changedElement ->
                val changesByElement = changeSet.getChangesByElement(changedElement)
                changesByElement.affectedChildren.all { affectedChild ->
                    if (!precondition(affectedChild)) return@all false
                    val changeByChild = changesByElement.getChangeByChild(affectedChild)
                    return@all if (changeByChild is ChangeInfoImpl) {
                        val oldChild = changeByChild.oldChild
                        precondition(oldChild)
                    } else false
                }
            }

        private fun isCommentChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiComment || it is KDoc }

        private fun isFormattingChange(changeSet: TreeChangeEvent): Boolean =
            isSpecificChange(changeSet) { it is PsiWhiteSpace }

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
                lambda.getTopmostParentOfType<KtSuperTypeCallEntry>()?.getTopmostParentOfType<KtClassOrObject>()?.let {
                    return BlockModificationScopeElement(it, it)
                }
            }

            val blockDeclaration =
                KtPsiUtil.getTopmostParentOfTypes(element, *BLOCK_DECLARATION_TYPES) as? KtDeclaration ?: return null
//                KtPsiUtil.getTopmostParentOfType<KtClassOrObject>(element) as? KtDeclaration ?: return null

            // should not be local declaration
            if (KtPsiUtil.isLocal(blockDeclaration))
                return null

            when (blockDeclaration) {
                is KtNamedFunction -> {
//                    if (blockDeclaration.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) {
//                        topClassLikeDeclaration(blockDeclaration)?.let {
//                            return BlockModificationScopeElement(it, it)
//                        }
//                    }
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
//                    if (blockDeclaration.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE) {
//                        topClassLikeDeclaration(blockDeclaration)?.let {
//                            return BlockModificationScopeElement(it, it)
//                        }
//                    }
                    if (blockDeclaration.typeReference != null) {
                        val accessors =
                            blockDeclaration.accessors.map { it.initializer ?: it.bodyExpression }

                        val accessorList = if (blockDeclaration.initializer.isAncestor(element) &&
                            // call expression changes in property initializer are OCB, see KT-38443
                            KtPsiUtil.getTopmostParentOfTypes(element, KtCallExpression::class.java) == null
                        ) {
                            accessors + blockDeclaration.initializer
                        } else {
                            accessors
                        }

                        for (accessor in accessorList) {
                            accessor?.takeIf {
                                it.isAncestor(element) &&
                                        // adding annotations to accessor is the same as change contract of property
                                        (element !is KtAnnotated || element.annotationEntries.isEmpty())
                            }
                                ?.let { expression ->
                                    val declaration =
                                        KtPsiUtil.getTopmostParentOfTypes(blockDeclaration, KtClassOrObject::class.java) as? KtElement ?:
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
                            (PsiTreeUtil.getParentOfType(blockDeclaration, KtClassOrObject::class.java))?.let {
                                return BlockModificationScopeElement(it, ktClassInitializer)
                            }
                        }
                }

                is KtSecondaryConstructor -> {
                    blockDeclaration
                        ?.takeIf {
                            it.bodyExpression?.isAncestor(element) ?: false || it.getDelegationCallOrNull()?.isAncestor(element) ?: false
                        }?.let { ktConstructor ->
                            PsiTreeUtil.getParentOfType(blockDeclaration, KtClassOrObject::class.java)?.let {
                                return BlockModificationScopeElement(it, ktConstructor)
                            }
                        }
                }
//                is KtClassOrObject -> {
//                    return when (element) {
//                        is KtProperty, is KtNamedFunction -> {
//                            if ((element as? KtModifierListOwner)?.visibilityModifierType()?.toVisibility() == Visibilities.PRIVATE)
//                                BlockModificationScopeElement(blockDeclaration, blockDeclaration) else null
//                        }
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
    }
}

private val PER_FILE_MODIFICATION_TRACKER = Key<SimpleModificationTracker>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.perFileModificationTracker: ModificationTracker
    get() = putUserDataIfAbsent(PER_FILE_MODIFICATION_TRACKER, SimpleModificationTracker())

private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.outOfBlockModificationCount: Long by NotNullableUserDataProperty(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, 0)

private fun KtFile.incOutOfBlockModificationCount() {
    clearInBlockModifications()

    val count = getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0
    putUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, count + 1)
}

/**
 * inBlockModifications is a collection of block elements those have in-block modifications
 */
private val IN_BLOCK_MODIFICATIONS = Key<MutableCollection<KtElement>>("IN_BLOCK_MODIFICATIONS")
private val FILE_IN_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_IN_BLOCK_MODIFICATION_COUNT")

val KtFile.inBlockModificationCount: Long by NotNullableUserDataProperty(FILE_IN_BLOCK_MODIFICATION_COUNT, 0)

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
    val count = getUserData(FILE_IN_BLOCK_MODIFICATION_COUNT) ?: 0
    putUserData(FILE_IN_BLOCK_MODIFICATION_COUNT, count + 1)
}

fun KtFile.clearInBlockModifications() {
    val collection = getUserData(IN_BLOCK_MODIFICATIONS)
    collection?.let {
        synchronized(it) {
            it.clear()
        }
    }
}