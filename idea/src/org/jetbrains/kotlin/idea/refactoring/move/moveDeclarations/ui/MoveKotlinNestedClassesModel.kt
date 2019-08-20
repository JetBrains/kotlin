/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement

internal class MoveKotlinNestedClassesModel(
    val project: Project,
    val openInEditorCheckBox: Boolean,
    val selectedElementsToMove: List<KtClassOrObject>,
    val originalClass: KtClassOrObject,
    val targetClass: PsiElement?,
    val moveCallback: MoveCallback?
) : Model<MoveKotlinDeclarationsProcessor> {

    private fun getCheckedTargetClass(): KtElement {
        val targetClass = this.targetClass ?: throw ConfigurationException("No destination class specified")

        if (targetClass !is KtClassOrObject){
            throw ConfigurationException("Destination class must be a Kotlin class")
        }

        if (originalClass === targetClass) {
            throw ConfigurationException(RefactoringBundle.message("source.and.destination.classes.should.be.different"))
        }

        for (classOrObject in selectedElementsToMove) {
            if (PsiTreeUtil.isAncestor(classOrObject, targetClass, false)) {
                throw ConfigurationException("Cannot move nested class ${classOrObject.name} to itself")
            }
        }

        return targetClass
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): MoveKotlinDeclarationsProcessor {
        val elementsToMove = selectedElementsToMove
        val target = KotlinMoveTargetForExistingElement(getCheckedTargetClass())
        val delegate = MoveDeclarationsDelegate.NestedClass()
        val descriptor = MoveDeclarationsDescriptor(
            project,
            MoveSource(elementsToMove),
            target,
            delegate,
            searchInCommentsAndStrings = false,
            searchInNonCode = false,
            deleteSourceFiles = false,
            moveCallback = moveCallback,
            openInEditor = openInEditorCheckBox
        )

        return MoveKotlinDeclarationsProcessor(descriptor, Mover.Default, throwOnConflicts)
    }
}