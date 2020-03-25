/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

object FirErrors {
    val UNRESOLVED_REFERENCE by error1<FirSourceElement, String?>()
    val INAPPLICABLE_CANDIDATE by error1<FirSourceElement, Collection<AbstractFirBasedSymbol<*>>>()
    val AMBIGUITY by error1<FirSourceElement, Collection<AbstractFirBasedSymbol<*>>>()
    val ASSIGN_OPERATOR_AMBIGUITY by error1<FirSourceElement, Collection<AbstractFirBasedSymbol<*>>>()
    val SYNTAX_ERROR by error0<FirSourceElement>()
    val UNRESOLVED_LABEL by error0<FirSourceElement>()
    val ILLEGAL_CONST_EXPRESSION by error0<FirSourceElement>()
    val DESERIALIZATION_ERROR by error0<FirSourceElement>()
    val INFERENCE_ERROR by error0<FirSourceElement>()
    val NO_SUPERTYPE by error0<FirSourceElement>()
    val TYPE_PARAMETER_AS_SUPERTYPE by error0<FirSourceElement>()
    val ENUM_AS_SUPERTYPE by error0<FirSourceElement>()
    val RECURSION_IN_SUPERTYPES by error0<FirSourceElement>()
    val RECURSION_IN_IMPLICIT_TYPES by error0<FirSourceElement>()
    val ERROR_FROM_JAVA_RESOLUTION by error0<FirSourceElement>()
    val OTHER_ERROR by error0<FirSourceElement>()
    val TYPE_MISMATCH by error2<FirSourceElement, ConeKotlinType, ConeKotlinType>()
    val VARIABLE_EXPECTED by error0<FirSourceElement>()
    val RETURN_NOT_ALLOWED by error0<FirSourceElement>()
    val CONSTRUCTOR_IN_OBJECT by error0<FirSourceElement>()
    val SUPER_IS_NOT_AN_EXPRESSION by error0<FirSourceElement>()

    val INAPPLICABLE_INFIX_MODIFIER by existing<FirSourceElement, String>(Errors.INAPPLICABLE_INFIX_MODIFIER)
}
