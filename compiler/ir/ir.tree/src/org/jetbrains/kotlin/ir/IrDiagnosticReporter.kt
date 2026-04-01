/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.fqNameWithoutFileClassesWhenAvailable

interface IrDiagnosticReporter {
    fun at(irDeclaration: IrDeclaration): IrDiagnosticContext
    fun at(irElement: IrElement, containingIrFile: IrFile): IrDiagnosticContext
    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): IrDiagnosticContext
    fun at(sourceElement: AbstractKtSourceElement?, irElement: IrElement, containingFile: IrFile): IrDiagnosticContext

    fun report(factory: KtSourcelessDiagnosticFactory, message: String)
    val hasErrors: Boolean

    interface IrDiagnosticContext : DiagnosticContext {
        val sourceElement: AbstractKtSourceElement?

        fun report(factory: KtDiagnosticFactory0)

        fun <A : Any> report(factory: KtDiagnosticFactory1<A>, a: A)

        fun <A : Any, B : Any> report(factory: KtDiagnosticFactory2<A, B>, a: A, b: B)

        fun <A : Any> report(factory: KtDiagnosticFactoryForDeprecation1<A>, a: A) {
            report(factory.chooseFactory(this), a)
        }

        fun <A : Any, B : Any> report(factory: KtDiagnosticFactoryForDeprecation2<A, B>, a: A, b: B) {
            report(factory.chooseFactory(this), a, b)
        }

        fun <A : Any, B : Any, C : Any> report(factory: KtDiagnosticFactoryForDeprecation3<A, B, C>, a: A, b: B, c: C) {
            report(factory.chooseFactory(this), a, b, c)
        }

        fun <A : Any, B : Any, C : Any> report(factory: KtDiagnosticFactory3<A, B, C>, a: A, b: B, c: C)

        abstract override fun equals(other: Any?): Boolean
        abstract override fun hashCode(): Int
    }

}

object IrDiagnosticRenderers {
    val SYMBOL_OWNER_DECLARATION_FQ_NAME = Renderer<IrSymbol> {
        (it.owner as? IrDeclarationWithName)?.fqNameWithoutFileClassesWhenAvailable?.asString() ?: "unknown name"
    }
    val DECLARATION_NAME = Renderer<IrDeclarationWithName> { it.name.asString() }

    /**
     * Inspired by [org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL_KIND].
     */
    val DECLARATION_KIND = Renderer<IrDeclaration> { declaration ->
        when (declaration) {
            is IrSimpleFunction -> when {
                declaration.isPropertyAccessor -> "property accessor"
                else -> "function"
            }
            is IrConstructor -> "constructor"
            is IrProperty -> "property"
            is IrClass -> declaration.kind.codeRepresentation ?: "declaration"
            else -> "declaration"
        }
    }

    val DECLARATION_KIND_AND_NAME = Renderer<IrDeclaration> { declaration ->
        "${DECLARATION_KIND.render(declaration)} '${(declaration as? IrDeclarationWithName)?.fqNameWhenAvailable?.asString()}'"
    }
}
