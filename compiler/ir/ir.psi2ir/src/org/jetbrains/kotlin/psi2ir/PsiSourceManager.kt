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

package org.jetbrains.kotlin.psi2ir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiIrFileEntry
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
