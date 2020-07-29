/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.NULLABLE_STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.PROPERTY_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.SYMBOLS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ASSIGN_OPERATOR_AMBIGUITY
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DEPRECATED_MODIFIER_PAIR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.DESERIALIZATION_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ENUM_AS_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ERROR_FROM_JAVA_RESOLUTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_FUNCTION_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_PROPERTY_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_RECEIVER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.HIDDEN
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_CONST_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ILLEGAL_UNDERSCORE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INAPPLICABLE_CANDIDATE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INCOMPATIBLE_MODIFIERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.INFERENCE_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NONE_APPLICABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.OTHER_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_IMPLICIT_TYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.RECURSION_IN_SUPERTYPES
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REPEATED_MODIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SUPER_IS_NOT_AN_EXPRESSION
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SYNTAX_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.TYPE_PARAMETER_AS_SUPERTYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNINITIALIZED_VARIABLE
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_LABEL
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.UNRESOLVED_REFERENCE

@Suppress("unused")
class FirDefaultErrorMessages : DefaultErrorMessages.Extension {
    override fun getMap(): DiagnosticFactoryToRendererMap {
        return MAP.psiDiagnosticMap
    }

    companion object {
        val MAP = FirDiagnosticFactoryToRendererMap("FIR").also { map ->
            map.put(UNRESOLVED_REFERENCE, "Unresolved reference: {0}", NULLABLE_STRING)
            map.put(HIDDEN, "Symbol {0} is invisible", SYMBOL)
            map.put(NONE_APPLICABLE, "None of the following functions are applicable: {0}", SYMBOLS)
            map.put(INAPPLICABLE_CANDIDATE, "Inapplicable candidate(s): {0}", SYMBOL)
            map.put(AMBIGUITY, "Ambiguity between candidates: {0}", SYMBOLS)
            map.put(ASSIGN_OPERATOR_AMBIGUITY, "Ambiguity between assign operator candidates: {0}", SYMBOLS)
            map.put(SYNTAX_ERROR, "Syntax error")
            map.put(UNRESOLVED_LABEL, "Unresolved label")
            map.put(ILLEGAL_CONST_EXPRESSION, "Illegal const expression")
            map.put(ILLEGAL_UNDERSCORE, "Illegal underscore")
            map.put(DESERIALIZATION_ERROR, "Deserialization error")
            map.put(INFERENCE_ERROR, "Inference error")
            map.put(TYPE_PARAMETER_AS_SUPERTYPE, "Type parameter as supertype")
            map.put(ENUM_AS_SUPERTYPE, "Enum as supertype")
            map.put(RECURSION_IN_SUPERTYPES, "Recursion in supertypes")
            map.put(RECURSION_IN_IMPLICIT_TYPES, "Recursion in implicit types")
            map.put(ERROR_FROM_JAVA_RESOLUTION, "Java resolution error")
            map.put(OTHER_ERROR, "Unknown (other) error")
            map.put(SUPER_IS_NOT_AN_EXPRESSION, "Super cannot be a callee")
            map.put(REPEATED_MODIFIER, "Repeated ''{0}''", TO_STRING)
            map.put(REDUNDANT_MODIFIER, "Modifier ''{0}'' is redundant because ''{1}'' is present", TO_STRING, TO_STRING)
            map.put(DEPRECATED_MODIFIER_PAIR, "Modifier ''{0}'' is deprecated in presence of ''{1}''", TO_STRING, TO_STRING)
            map.put(INCOMPATIBLE_MODIFIERS, "Modifier ''{0}'' is incompatible with ''{1}''", TO_STRING, TO_STRING)

            // Exposed visibility group
            map.put(EXPOSED_TYPEALIAS_EXPANDED_TYPE, "{0} typealias exposes {2} in expanded type{1}", TO_STRING, TO_STRING, TO_STRING)
            map.put(EXPOSED_PROPERTY_TYPE, "{0} property exposes its {2} type{1}", TO_STRING, TO_STRING, TO_STRING)
            map.put(EXPOSED_FUNCTION_RETURN_TYPE, "{0} function exposes its {2} return type{1}", TO_STRING, TO_STRING, TO_STRING)
            map.put(EXPOSED_RECEIVER_TYPE, "{0} member exposes its {2} receiver type{1}", TO_STRING, TO_STRING, TO_STRING)
            map.put(EXPOSED_PARAMETER_TYPE, "{0} function exposes its {2} parameter type{1}", TO_STRING, TO_STRING, TO_STRING)

            // Control flow diagnostics
            map.put(UNINITIALIZED_VARIABLE, "{2} must be initialized before access", PROPERTY_NAME)
        }
    }
}
