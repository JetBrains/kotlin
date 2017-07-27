/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.pom.PomManager
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.CommonProcessors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parents

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
class KotlinCodeBlockModificationListener(
        modificationTracker: PsiModificationTracker,
        project: Project,
        private val treeAspect: TreeAspect
) {
    private val perModuleModCount = mutableMapOf<Module, Long>()
    private val modificationTrackerImpl = modificationTracker as PsiModificationTrackerImpl

    private var lastAffectedModule: Module? = null
    private var lastAffectedModuleModCount = -1L

    // All modifications since that count are known to be single-module modifications reflected in
    // perModuleModCount map
    private var perModuleChangesHighwatermark: Long? = null

    fun getModificationCount(module: Module): Long {
        return perModuleModCount[module] ?:
               perModuleChangesHighwatermark ?:
               modificationTrackerImpl.outOfCodeBlockModificationCount
    }

    fun hasPerModuleModificationCounts() = perModuleChangesHighwatermark != null

    init {
        val model = PomManager.getModel(project)
        val messageBusConnection = project.messageBus.connect()
        model.addModelListener(object: PomModelListener {
            override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean {
                return aspect == treeAspect
            }

            override fun modelChanged(event: PomModelEvent) {
                val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return
                val file = changeSet.rootElement.psi.containingFile as? KtFile ?: return
                val changedElements = changeSet.changedElements
                // When a code fragment is reparsed, IntelliJ doesn't do an AST diff and considers the entire
                // contents to be replaced, which is represented in a POM event as an empty list of changed elements
                if (changedElements.any { getInsideCodeBlockModificationScope(it.psi) == null } ||
                    (file is PsiCodeFragment && changedElements.isEmpty())) {
                    messageBusConnection.deliverImmediately()
                    if (file.isPhysical) {
                        lastAffectedModule = ModuleUtil.findModuleForPsiElement(file)
                        lastAffectedModuleModCount = modificationTrackerImpl.outOfCodeBlockModificationCount
                        modificationTrackerImpl.incCounter()
                    }
                    incOutOfBlockModificationCount(file)
                }
            }
        })

        messageBusConnection.subscribe(PsiModificationTracker.TOPIC, PsiModificationTracker.Listener {
            val newModCount = modificationTrackerImpl.outOfCodeBlockModificationCount
            val affectedModule = lastAffectedModule
            if (affectedModule != null && newModCount == lastAffectedModuleModCount + 1) {
                if (perModuleChangesHighwatermark== null) {
                    perModuleChangesHighwatermark = lastAffectedModuleModCount
                }
                perModuleModCount[affectedModule] = newModCount
            }
            else {
                perModuleChangesHighwatermark = null
                perModuleModCount.clear()
            }
        })
    }

    companion object {
        private fun incOutOfBlockModificationCount(file: KtFile) {
            val count = file.getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0
            file.putUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, count + 1)
        }

        fun getInsideCodeBlockModificationScope(element: PsiElement): KtElement? {
            val lambda = element.getTopmostParentOfType<KtLambdaExpression>()
            if (lambda is KtLambdaExpression) {
                lambda.getTopmostParentOfType<KtSuperTypeCallEntry>()
                        ?.let { return it }
            }

            val blockDeclaration = KtPsiUtil.getTopmostParentOfTypes(element, *BLOCK_DECLARATION_TYPES) ?: return null
            if (blockDeclaration.parents.any { it !is KtClassBody && it !is KtClassOrObject && it !is KtFile }) return null // should not be local declaration

            when (blockDeclaration) {
                is KtNamedFunction -> {
                    if (blockDeclaration.hasBlockBody()) {
                        return blockDeclaration.bodyExpression?.takeIf { it.isAncestor(element) }
                    }
                    else if (blockDeclaration.hasDeclaredReturnType()) {
                        return blockDeclaration.initializer?.takeIf { it.isAncestor(element) }
                    }
                }

                is KtProperty -> {
                    if (blockDeclaration.typeReference != null) {
                        for (accessor in blockDeclaration.accessors) {
                            (accessor.initializer ?: accessor.bodyExpression)
                                    ?.takeIf { it.isAncestor(element) }
                                    ?.let { return it }
                        }
                    }
                }

                else -> throw IllegalStateException()
            }

            return null
        }

        fun isBlockDeclaration(declaration: KtDeclaration): Boolean {
            return BLOCK_DECLARATION_TYPES.any { it.isInstance(declaration) }
        }

        private val BLOCK_DECLARATION_TYPES = arrayOf<Class<out KtDeclaration>>(
                KtProperty::class.java,
                KtNamedFunction::class.java
        )

        fun getInstance(project: Project) = project.getComponent(KotlinCodeBlockModificationListener::class.java)
    }
}

private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.outOfBlockModificationCount: Long
    get() = getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0

class KotlinModuleModificationTracker(val module: Module): ModificationTracker {
    private val kotlinModCountListener = KotlinCodeBlockModificationListener.getInstance(module.project)
    private val psiModificationTracker = PsiModificationTracker.SERVICE.getInstance(module.project)
    private val dependencies by lazy {
        HashSet<Module>().apply {
            ModuleRootManager.getInstance(module).orderEntries().recursively().forEachModule(
                    CommonProcessors.CollectProcessor(this))
        }
    }

    override fun getModificationCount(): Long {
        val currentGlobalCount = psiModificationTracker.outOfCodeBlockModificationCount

        if (kotlinModCountListener.hasPerModuleModificationCounts()) {
            val selfCount = kotlinModCountListener.getModificationCount(module)
            if (selfCount == currentGlobalCount) return selfCount
            var maxCount = selfCount
            for (dependency in dependencies) {
                val depCount = kotlinModCountListener.getModificationCount(dependency)
                if (depCount == currentGlobalCount) return currentGlobalCount
                if (depCount > maxCount) maxCount = depCount
            }
            return maxCount
        }
        return currentGlobalCount
    }
}
