/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ASSIGN_OPERATOR_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DESERIALIZATION_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ENUM_AS_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ERROR_FROM_JAVA_RESOLUTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_CONST_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_CANDIDATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INFERENCE_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NO_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OTHER_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_IMPLICIT_TYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_SUPERTYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SYNTAX_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_LABEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_REFERENCE
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

@Suppress("unused")
class DefaultErrorMessagesFir : DefaultErrorMessages.Extension {

    override fun getMap(): DiagnosticFactoryToRendererMap {
        return MAP
    }

    companion object {
        private val SYMBOL_COLLECTION_RENDERER = Renderer { symbols: Collection<AbstractFirBasedSymbol<*>> ->
            symbols.joinToString(prefix = "[", postfix = "]", separator = ",", limit = 3, truncated = "...") { symbol ->
                when (symbol) {
                    is FirClassLikeSymbol<*> -> symbol.classId.asString()
                    is FirCallableSymbol<*> -> symbol.callableId.toString()
                    else -> "???"
                }
            }
        }

        private val MAP = DiagnosticFactoryToRendererMap("FIR").also { map ->
            map.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", Renderer { it })
            map.put(INAPPLICABLE_CANDIDATE, "Inapplicable candidate(s): {0}", SYMBOL_COLLECTION_RENDERER)
            map.put(AMBIGUITY, "Ambiguity between candidates: {0}", SYMBOL_COLLECTION_RENDERER)
            map.put(ASSIGN_OPERATOR_AMBIGUITY, "Ambiguity between assign operator candidates: {0}", SYMBOL_COLLECTION_RENDERER)
            map.put(SYNTAX_ERROR, "Syntax error")
            map.put(UNRESOLVED_LABEL, "Unresolved label")
            map.put(ILLEGAL_CONST_EXPRESSION, "Illegal const expression")
            map.put(DESERIALIZATION_ERROR, "Deserialization error")
            map.put(INFERENCE_ERROR, "Inference error")
            map.put(NO_SUPERTYPE, "No supertype")
            map.put(TYPE_PARAMETER_AS_SUPERTYPE, "Type parameter as supertype")
            map.put(ENUM_AS_SUPERTYPE, "Enum as supertype")
            map.put(RECURSION_IN_SUPERTYPES, "Recursion in supertypes")
            map.put(RECURSION_IN_IMPLICIT_TYPES, "Recursion in implicit types")
            map.put(ERROR_FROM_JAVA_RESOLUTION, "Java resolution error")
            map.put(OTHER_ERROR, "Unknown (other) error")

        }
    }
}