/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

object FirErrors {
    val UNRESOLVED_REFERENCE = DiagnosticFactory1.create<KtElement, String?>(ERROR)
    val INAPPLICABLE_CANDIDATE = DiagnosticFactory1.create<KtElement, Collection<AbstractFirBasedSymbol<*>>>(ERROR)
    val AMBIGUITY = DiagnosticFactory1.create<KtElement, Collection<AbstractFirBasedSymbol<*>>>(ERROR)
    val ASSIGN_OPERATOR_AMBIGUITY = DiagnosticFactory1.create<KtElement, Collection<AbstractFirBasedSymbol<*>>>(ERROR)
    val SYNTAX_ERROR = DiagnosticFactory0.create<KtElement>(ERROR)
    val UNRESOLVED_LABEL = DiagnosticFactory0.create<KtElement>(ERROR)
    val ILLEGAL_CONST_EXPRESSION = DiagnosticFactory0.create<KtElement>(ERROR)
    val DESERIALIZATION_ERROR = DiagnosticFactory0.create<KtElement>(ERROR)
    val INFERENCE_ERROR = DiagnosticFactory0.create<KtElement>(ERROR)
    val NO_SUPERTYPE = DiagnosticFactory0.create<KtElement>(ERROR)
    val TYPE_PARAMETER_AS_SUPERTYPE = DiagnosticFactory0.create<KtElement>(ERROR)
    val ENUM_AS_SUPERTYPE = DiagnosticFactory0.create<KtElement>(ERROR)
    val RECURSION_IN_SUPERTYPES = DiagnosticFactory0.create<KtElement>(ERROR)
    val RECURSION_IN_IMPLICIT_TYPES = DiagnosticFactory0.create<KtElement>(ERROR)
    val ERROR_FROM_JAVA_RESOLUTION = DiagnosticFactory0.create<KtElement>(ERROR)
    val OTHER_ERROR = DiagnosticFactory0.create<KtElement>(ERROR)
    val TYPE_MISMATCH = DiagnosticFactory2.create<KtElement, ConeKotlinType, ConeKotlinType>(ERROR)

    init {
        registerExternalFactories()
    }
}