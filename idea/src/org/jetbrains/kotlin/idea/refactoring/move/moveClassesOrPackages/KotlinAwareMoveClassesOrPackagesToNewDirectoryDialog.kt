/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveClassesOrPackages

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.refactoring.MoveDestination
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesToNewDirectoryDialog

class KotlinAwareMoveClassesOrPackagesToNewDirectoryDialog(
    directory: PsiDirectory,
    elementsToMove: Array<out PsiElement>,
    moveCallback: MoveCallback?
) : MoveClassesOrPackagesToNewDirectoryDialog(directory, elementsToMove, moveCallback) {
    override fun createDestination(aPackage: PsiPackage, directory: PsiDirectory): MoveDestination? {
        val delegate = super.createDestination(aPackage, directory) ?: return null
        return KotlinAwareDelegatingMoveDestination(delegate, aPackage, directory)
    }
}