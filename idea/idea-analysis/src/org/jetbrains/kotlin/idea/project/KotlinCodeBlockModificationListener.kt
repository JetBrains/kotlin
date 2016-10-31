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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.pom.PomManager
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
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
    init {
        val model = PomManager.getModel(project)
        @Suppress("NAME_SHADOWING")
        val modificationTracker = modificationTracker as PsiModificationTrackerImpl
        model.addModelListener(object: PomModelListener {
            override fun isAspectChangeInteresting(aspect: PomModelAspect): Boolean {
                return aspect == treeAspect
            }

            override fun modelChanged(event: PomModelEvent) {
                val changeSet = event.getChangeSet(treeAspect) as TreeChangeEvent? ?: return
                val file = changeSet.rootElement.psi.containingFile as? KtFile ?: return
                if (changeSet.changedElements.any { getInsideCodeBlockModificationScope(it.psi) == null }) {
                    if (file.isPhysical) {
                        modificationTracker.incCounter()
                    }
                    incOutOfBlockModificationCount(file)
                }
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
                    for (accessor in blockDeclaration.accessors) {
                        (accessor.initializer ?: accessor.bodyExpression)
                                ?.takeIf { it.isAncestor(element) }
                                ?.let { return it }
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
    }
}

private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")

val KtFile.outOfBlockModificationCount: Long
    get() = getUserData(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT) ?: 0
