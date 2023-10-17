/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.baseForIntersectionOverride
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.isSubstitutionOverride
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

open class FirResolvedNamedReferenceRenderer {

    internal lateinit var components: FirRendererComponents

    protected val printer: FirPrinter get() = components.printer
    private val visitor: FirRenderer.Visitor get() = components.visitor

    internal open fun render(resolvedNamedReference: FirResolvedNamedReference) {
        val symbol = resolvedNamedReference.resolvedSymbol
        val isSubstitutionOverride = (symbol.fir as? FirCallableDeclaration)?.isSubstitutionOverride == true

        if (isSubstitutionOverride) {
            printer.print("SubstitutionOverride<")
        }

        components.referencedSymbolRenderer.printReference(symbol.unwrapIntersectionOverrides())

        if (resolvedNamedReference is FirResolvedCallableReference) {
            if (resolvedNamedReference.inferredTypeArguments.isNotEmpty()) {
                printer.print("<")
                for ((index, element) in resolvedNamedReference.inferredTypeArguments.withIndex()) {
                    if (index > 0) {
                        printer.print(", ")
                    }
                    components.typeRenderer.render(element)
                }
                printer.print(">")
            }
        }

        if (isSubstitutionOverride) {
            when (symbol) {
                is FirNamedFunctionSymbol -> {
                    printer.print(": ")
                    symbol.fir.returnTypeRef.accept(visitor)
                }
                is FirPropertySymbol -> {
                    printer.print(": ")
                    symbol.fir.returnTypeRef.accept(visitor)
                }
            }
            printer.print(">")
        }
        if (resolvedNamedReference is FirResolvedErrorReference) {
            printer.print("<${resolvedNamedReference.diagnostic.reason}>#")
        }
    }

    private fun FirBasedSymbol<*>.unwrapIntersectionOverrides(): FirBasedSymbol<*> {
        (this as? FirCallableSymbol<*>)?.baseForIntersectionOverride?.let { return it.unwrapIntersectionOverrides() }
        return this
    }
}

class FirResolvedNamedReferenceRendererWithLabel : FirResolvedNamedReferenceRenderer() {
    override fun render(resolvedNamedReference: FirResolvedNamedReference) {
        printer.print("R|")
        super.render(resolvedNamedReference)
        printer.print("|")
    }
}