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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.BundleBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.RadioUpDownListener
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import java.awt.BorderLayout
import javax.swing.*

internal class KotlinSelectNestedClassRefactoringDialog private constructor (
        private val project: Project,
        private val nestedClass: KtClassOrObject,
        private val targetContainer: PsiElement?
) : DialogWrapper(project, true) {
    private val moveToUpperLevelButton = JRadioButton()
    private val moveMembersButton = JRadioButton()

    init {
        title = RefactoringBundle.message("select.refactoring.title")
        init()
    }

    override fun createNorthPanel() = JLabel(RefactoringBundle.message("what.would.you.like.to.do"))

    override fun getPreferredFocusedComponent() = moveToUpperLevelButton

    override fun getDimensionServiceKey(): String {
        return "#org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui.KotlinSelectInnerOrMembersRefactoringDialog"
    }

    override fun createCenterPanel(): JComponent? {
        moveToUpperLevelButton.text = BundleBase.replaceMnemonicAmpersand("Move &nested class ${nestedClass.name} to upper level")
        moveToUpperLevelButton.isSelected = true

        moveMembersButton.text = BundleBase.replaceMnemonicAmpersand("&Move nested class ${nestedClass.name} to another class")

        ButtonGroup().apply {
            add(moveToUpperLevelButton)
            add(moveMembersButton)
        }

        RadioUpDownListener(moveToUpperLevelButton, moveMembersButton)

        return JPanel(BorderLayout()).apply {
            val box = Box.createVerticalBox().apply {
                add(Box.createVerticalStrut(5))
                add(moveToUpperLevelButton)
                add(moveMembersButton)
            }
            add(box, BorderLayout.CENTER)
        }
    }

    fun getNextDialog(): DialogWrapper? {
        return when {
            moveToUpperLevelButton.isSelected -> MoveKotlinNestedClassesToUpperLevelDialog(nestedClass, targetContainer)
            moveMembersButton.isSelected -> MoveKotlinNestedClassesDialog(nestedClass, targetContainer)
            else -> null
        }
    }

    companion object {
        private fun MoveKotlinNestedClassesToUpperLevelDialog(
                nestedClass: KtClassOrObject,
                targetContainer: PsiElement?
        ): MoveKotlinNestedClassesToUpperLevelDialog {
            val outerClass = nestedClass.containingClassOrObject!!
            val newTarget = targetContainer
                            ?: outerClass.containingClassOrObject
                            ?: outerClass.containingFile.let { it.containingDirectory ?: it }
            return MoveKotlinNestedClassesToUpperLevelDialog(nestedClass.project, nestedClass, newTarget)
        }

        private fun MoveKotlinNestedClassesDialog(
                nestedClass: KtClassOrObject,
                targetContainer: PsiElement?
        ): MoveKotlinNestedClassesDialog {
            return MoveKotlinNestedClassesDialog(nestedClass.project,
                                                 listOf(nestedClass),
                                                 nestedClass.containingClassOrObject!!,
                                                 targetContainer as? KtClassOrObject ?: nestedClass.containingClassOrObject!!,
                                                 null)
        }

        fun chooseNestedClassRefactoring(nestedClass: KtClassOrObject, targetContainer: PsiElement?) {
            val project = nestedClass.project
            val dialog = when {
                targetContainer != null && targetContainer !is KtClassOrObject ||
                nestedClass is KtClass && nestedClass.isInner() -> {
                    MoveKotlinNestedClassesToUpperLevelDialog(nestedClass, targetContainer)
                }
                else -> {
                    val selectionDialog = KotlinSelectNestedClassRefactoringDialog(project, nestedClass, targetContainer)
                    selectionDialog.show()
                    if (selectionDialog.exitCode != OK_EXIT_CODE) return
                    selectionDialog.getNextDialog() ?: return
                }
            }
            dialog.show()
        }
    }
}