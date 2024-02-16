/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.name.SpecialNames

open class FirValueParameterRenderer {
    internal lateinit var components: FirRendererComponents
    protected val printer get() = components.printer
    protected val visitor get() = components.visitor
    private val annotationRenderer get() = components.annotationRenderer
    protected val declarationRenderer get() = components.declarationRenderer
    private val modifierRenderer get() = components.modifierRenderer
    protected val typeRenderer get() = components.typeRenderer

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
            printer.print(valueParameter.name.toString() + ": ")
        }

        renderParameterType(valueParameter)
        renderDefaultValue(valueParameter)
    }

    protected open fun renderParameterType(valueParameter: FirValueParameter) {
        valueParameter.returnTypeRef.accept(visitor)
    }

    protected open fun renderDefaultValue(valueParameter: FirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ")
            it.accept(visitor)
        }
    }
}
