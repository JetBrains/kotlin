/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.isLegacyContextReceiver
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.SpecialNames

open class FirCallableSignatureRenderer {
    internal lateinit var components: FirRendererComponents
    protected val printer: FirPrinter get() = components.printer
    protected val visitor: FirRenderer.Visitor get() = components.visitor
    private val annotationRenderer: FirAnnotationRenderer? get() = components.annotationRenderer
    protected val declarationRenderer: FirDeclarationRenderer? get() = components.declarationRenderer
    private val modifierRenderer: FirModifierRenderer? get() = components.modifierRenderer
    protected val typeRenderer: ConeTypeRenderer get() = components.typeRenderer

    fun renderParameters(valueParameters: List<FirValueParameter>) {
        printer.print("(")
        for ((index, valueParameter) in valueParameters.withIndex()) {
            if (index > 0) {
                printer.print(", ")
            }
            renderParameter(valueParameter)
        }
        printer.print(")")
    }

    fun renderParameter(valueParameter: FirValueParameter) {
        declarationRenderer?.renderPhaseAndAttributes(valueParameter)
        annotationRenderer?.render(valueParameter)
        modifierRenderer?.renderModifiers(valueParameter)
        if (valueParameter.name != SpecialNames.NO_NAME_PROVIDED) {
            if (valueParameter.isLegacyContextReceiver()) {
                val typeName = when (val typeRef = valueParameter.returnTypeRef) {
                    is FirUserTypeRef -> typeRef.qualifier.lastOrNull()?.name
                    is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName
                    else -> null
                }

                if (valueParameter.name != typeName) {
                    printer.print(valueParameter.name)
                    printer.print("@")
                }
            } else {
                printer.print(valueParameter.name.toString())
                renderReturnTypePrefix()
            }
        }

        renderCallableType(valueParameter)
        renderDefaultValue(valueParameter)
    }

    open fun renderCallableType(callableDeclaration: FirCallableDeclaration) {
        callableDeclaration.returnTypeRef.accept(visitor)
    }

    open fun renderReturnTypePrefix() {
        printer.print(": ")
    }

    protected open fun renderDefaultValue(valueParameter: FirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ")
            it.accept(visitor)
        }
    }
}
