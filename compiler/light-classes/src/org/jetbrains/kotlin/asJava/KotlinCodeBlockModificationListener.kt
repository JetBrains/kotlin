/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl
import com.intellij.psi.impl.PsiTreeChangeEventImpl.PsiEventType.*
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

/**
 * Tested in OutOfBlockModificationTestGenerated
 */
public class KotlinCodeBlockModificationListener(modificationTracker: PsiModificationTracker) : PsiTreeChangePreprocessor {
    private val myModificationTracker = modificationTracker as PsiModificationTrackerImpl

    override fun treeChanged(event: PsiTreeChangeEventImpl) {
        if (event.file !is JetFile) return

        when (event.code) {
            BEFORE_CHILDREN_CHANGE,
            BEFORE_PROPERTY_CHANGE,
            BEFORE_CHILD_MOVEMENT,
            BEFORE_CHILD_REPLACEMENT,
            BEFORE_CHILD_ADDITION,
            BEFORE_CHILD_REMOVAL -> {
                // skip
            }

            CHILD_ADDED,
            CHILD_REMOVED,
            CHILD_REPLACED -> {
                processChange(event.parent, event.oldChild, event.child)
            }

            CHILDREN_CHANGED -> {
                if (!event.isGenericChange) {
                    processChange(event.parent, event.parent, null)
                }
            }

            CHILD_MOVED,
            PROPERTY_CHANGED -> {
                myModificationTracker.incCounter()
            }

            else -> LOG.error("Unknown code:" + event.code)
        }
    }

    private fun processChange(parent: PsiElement?, child1: PsiElement?, child2: PsiElement?) {
        try {
            if (!isInsideCodeBlock(parent)) {
                if (parent != null && parent.containingFile is JetFile) {
                    myModificationTracker.incCounter()
                }
                else {
                    myModificationTracker.incOutOfCodeBlockModificationCounter()
                }
                return
            }

            if (containsClassesInside(child1) || (child2 != child1 && containsClassesInside(child2))) {
                myModificationTracker.incCounter()
            }
        }
        catch (e: PsiInvalidElementAccessException) {
            myModificationTracker.incCounter() // Shall not happen actually, just a pre-release paranoia
        }
    }

    companion object {
        private val LOG = Logger.getInstance("#org.jetbrains.kotlin.asJava.KotlinCodeBlockModificationListener")

        private fun containsClassesInside(element: PsiElement?): Boolean {
            if (element == null) return false
            if (element is PsiClass) return true

            var child = element.firstChild
            while (child != null) {
                if (containsClassesInside(child)) return true
                child = child.nextSibling
            }

            return false
        }

        public fun isInsideCodeBlock(element: PsiElement?): Boolean {
            if (element is PsiFileSystemItem) return false
            if (element == null || element.parent == null) return true

            //TODO: other types
            val blockDeclaration = JetPsiUtil.getTopmostParentOfTypes(element, *BLOCK_DECLARATION_TYPES) ?: return false

            when (blockDeclaration) {
                is JetNamedFunction -> {
                    if (blockDeclaration.hasBlockBody()) {
                        return blockDeclaration.bodyExpression.isAncestor(element)
                    }
                    else if (blockDeclaration.hasDeclaredReturnType()) {
                        return blockDeclaration.initializer.isAncestor(element)
                    }
                }

                is JetProperty -> {
                    for (accessor in blockDeclaration.accessors) {
                        if (accessor.initializer.isAncestor(element) || accessor.bodyExpression.isAncestor(element)) {
                            return true
                        }
                    }
                }

                else -> throw IllegalStateException()
            }

            return false
        }

        public fun isBlockDeclaration(declaration: JetDeclaration): Boolean {
            return BLOCK_DECLARATION_TYPES.any { it.isInstance(declaration) }
        }

        private val BLOCK_DECLARATION_TYPES = arrayOf<Class<out JetDeclaration>>(
                javaClass<JetProperty>(),
                javaClass<JetNamedFunction>()
        )
    }
}
