/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.fileOrNull
import kotlin.reflect.KClass

object PsiSourceManager {
    fun <E : PsiElement> findPsiElement(irElement: IrElement, irFile: IrFile, psiElementClass: KClass<E>): E? {
        val psiFileEntry = irFile.fileEntry as? PsiIrFileEntry ?: return null
        return psiFileEntry.findPsiElement(irElement, psiElementClass)
    }

    fun findPsiElement(irElement: IrElement, irFile: IrFile): PsiElement? {
        val psiFileEntry = irFile.fileEntry as? PsiIrFileEntry ?: return null
        return psiFileEntry.findPsiElement(irElement)
    }

    fun <E : PsiElement> findPsiElement(irElement: IrElement, irDeclaration: IrDeclaration, psiElementClass: KClass<E>): E? {
        val irFile = irDeclaration.fileOrNull ?: return null
        return findPsiElement(irElement, irFile, psiElementClass)
    }

    fun findPsiElement(irElement: IrElement, irDeclaration: IrDeclaration): PsiElement? {
        val irFile = irDeclaration.fileOrNull ?: return null
        return findPsiElement(irElement, irFile)
    }

    fun <E : PsiElement> findPsiElement(irDeclaration: IrDeclaration, psiElementClass: KClass<E>): E? =
        findPsiElement(irDeclaration, irDeclaration, psiElementClass)

    fun findPsiElement(irDeclaration: IrDeclaration): PsiElement? =
        findPsiElement(irDeclaration, irDeclaration)
}
