/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.psi.PsiDirectory
import com.intellij.refactoring.MoveDestination
import org.jetbrains.kotlin.idea.core.getPackage

sealed class AutocreatingPsiDirectoryWrapper {
    class ByPsiDirectory(private val psiDirectory: PsiDirectory) : AutocreatingPsiDirectoryWrapper() {
        override fun getPackageName(): String = psiDirectory.getPackage()?.qualifiedName ?: ""
        override fun getOrCreateDirectory(source: PsiDirectory) = psiDirectory
    }

    class ByMoveDestination(private val moveDestination: MoveDestination) : AutocreatingPsiDirectoryWrapper() {
        override fun getPackageName() = moveDestination.targetPackage.qualifiedName
        override fun getOrCreateDirectory(source: PsiDirectory): PsiDirectory = moveDestination.getTargetDirectory(source)
    }

    abstract fun getPackageName(): String
    abstract fun getOrCreateDirectory(source: PsiDirectory): PsiDirectory
}

fun MoveDestination.toDirectoryWrapper() = AutocreatingPsiDirectoryWrapper.ByMoveDestination(this)
fun PsiDirectory.toDirectoryWrapper() = AutocreatingPsiDirectoryWrapper.ByPsiDirectory(this)