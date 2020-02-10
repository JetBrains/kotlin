/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.render
import kotlin.reflect.KClass

class PsiErrorBuilder(
    private val psiSourceManager: PsiSourceManager,
    private val diagnosticSink: DiagnosticSink
) {

    fun <E : PsiElement> at(irDeclaration: IrDeclaration, psiElementClass: KClass<E>): Location<E> =
        Location(
            psiSourceManager.findPsiElement(irDeclaration, psiElementClass)
                ?: throw AssertionError("No ${psiElementClass.simpleName} found for '${irDeclaration.render()}'")
        )

    fun at(irDeclaration: IrDeclaration): Location<PsiElement> =
        Location(
            psiSourceManager.findPsiElement(irDeclaration)
                ?: throw AssertionError("No PsiElement found for '${irDeclaration.render()}'")
        )

    fun <E : PsiElement> at(psiElement: E) = Location(psiElement)

    fun <E : PsiElement> at(irElement: IrElement, irDeclaration: IrDeclaration, psiElementClass: KClass<E>): Location<E> =
        Location(
            psiSourceManager.findPsiElement(irElement, irDeclaration, psiElementClass)
                ?: throw AssertionError("No ${psiElementClass.simpleName} found for '${irElement.render()}'")
        )

    fun at(irElement: IrElement, irDeclaration: IrDeclaration): Location<PsiElement> =
        Location(
            psiSourceManager.findPsiElement(irElement, irDeclaration)
                ?: throw AssertionError("No PsiElement found for '${irElement.render()}'")
        )

    fun <E : PsiElement> at(irElement: IrElement, irFile: IrFile, psiElementClass: KClass<E>): Location<E> =
        Location(
            psiSourceManager.findPsiElement(irElement, irFile, psiElementClass)
                ?: throw AssertionError("No ${psiElementClass.simpleName} found for '${irElement.render()}'")
        )

    fun at(irElement: IrElement, irFile: IrFile): Location<PsiElement> =
        Location(
            psiSourceManager.findPsiElement(irElement, irFile)
                ?: throw AssertionError("No PsiElement found for '${irElement.render()}'")
        )

    inner class Location<E : PsiElement>(private val psiElement: E) {

        fun report(diagnosticFactory: DiagnosticFactory0<E>) {
            diagnosticSink.report(diagnosticFactory.on(psiElement))
        }

        fun <A> report(diagnosticFactory: DiagnosticFactory1<E, A>, a: A) {
            diagnosticSink.report(diagnosticFactory.on(psiElement, a))
        }

        fun <A, B> report(diagnosticFactory: DiagnosticFactory2<E, A, B>, a: A, b: B) {
            diagnosticSink.report(diagnosticFactory.on(psiElement, a, b))
        }

        fun <A, B, C> report(diagnosticFactory: DiagnosticFactory3<E, A, B, C>, a: A, b: B, c: C) {
            diagnosticSink.report(diagnosticFactory.on(psiElement, a, b, c))
        }
    }
}