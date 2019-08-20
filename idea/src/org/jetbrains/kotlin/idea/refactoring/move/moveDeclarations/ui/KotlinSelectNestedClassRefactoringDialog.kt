/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import java.awt.BorderLayout
import javax.swing.*

internal class KotlinSelectNestedClassRefactoringDialog private constructor(
    project: Project,
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
        ): MoveKotlinNestedClassesToUpperLevelDialog? {
            val outerClass = nestedClass.containingClassOrObject ?: return null
            val newTarget = targetContainer
                ?: outerClass.containingClassOrObject
                ?: outerClass.containingFile.let { it.containingDirectory ?: it }
            return MoveKotlinNestedClassesToUpperLevelDialog(nestedClass.project, nestedClass, newTarget)
        }

        private fun MoveKotlinNestedClassesDialog(
            nestedClass: KtClassOrObject,
            targetContainer: PsiElement?
        ): MoveKotlinNestedClassesDialog {
            return MoveKotlinNestedClassesDialog(
                nestedClass.project,
                listOf(nestedClass),
                nestedClass.containingClassOrObject!!,
                targetContainer as? KtClassOrObject ?: nestedClass.containingClassOrObject!!,
                null
            )
        }

        fun chooseNestedClassRefactoring(nestedClass: KtClassOrObject, targetContainer: PsiElement?) {
            val project = nestedClass.project
            val dialog = when {
                targetContainer != null && targetContainer !is KtClassOrObject ||
                        nestedClass is KtClass && nestedClass.isInner() -> {
                    MoveKotlinNestedClassesToUpperLevelDialog(nestedClass, targetContainer)
                }
                nestedClass is KtEnumEntry -> return
                else -> {
                    val selectionDialog =
                        KotlinSelectNestedClassRefactoringDialog(
                            project,
                            nestedClass,
                            targetContainer
                        )
                    selectionDialog.show()
                    if (selectionDialog.exitCode != OK_EXIT_CODE) return
                    selectionDialog.getNextDialog() ?: return
                }
            }
            dialog?.show()
        }
    }
}