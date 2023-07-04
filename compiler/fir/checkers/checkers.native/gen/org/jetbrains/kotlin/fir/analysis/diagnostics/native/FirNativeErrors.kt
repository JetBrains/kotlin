/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitInvalidCharsInNativeIdentifiers
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirNativeErrors {
    // All
    val THROWS_LIST_EMPTY by error0<KtElement>()
    val INCOMPATIBLE_THROWS_OVERRIDE by error1<KtElement, FirRegularClassSymbol>()
    val INCOMPATIBLE_THROWS_INHERITED by error1<KtDeclaration, Collection<FirRegularClassSymbol>>()
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND by error1<KtElement, FqName>()
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY by error0<KtElement>()
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL by error0<KtElement>()
    val INAPPLICABLE_THREAD_LOCAL by error0<KtElement>()
    val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL by error0<KtElement>()
    val INVALID_CHARACTERS_NATIVE by deprecationError1<PsiElement, String>(ProhibitInvalidCharsInNativeIdentifiers, SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val REDUNDANT_SWIFT_REFINEMENT by error0<KtElement>()
    val INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE by error2<KtElement, FirBasedSymbol<*>, Collection<FirRegularClassSymbol>>()
    val INAPPLICABLE_OBJC_NAME by error0<KtElement>()
    val INVALID_OBJC_NAME by error0<KtElement>()
    val INVALID_OBJC_NAME_CHARS by error1<KtElement, String>()
    val INVALID_OBJC_NAME_FIRST_CHAR by error1<KtElement, String>()
    val EMPTY_OBJC_NAME by error0<KtElement>()
    val INCOMPATIBLE_OBJC_NAME_OVERRIDE by error2<KtElement, FirBasedSymbol<*>, Collection<FirRegularClassSymbol>>()
    val INAPPLICABLE_EXACT_OBJC_NAME by error0<KtElement>()
    val MISSING_EXACT_OBJC_NAME by error0<KtElement>()
    val NON_LITERAL_OBJC_NAME_ARG by error0<KtElement>()
    val INVALID_OBJC_HIDES_TARGETS by error0<KtElement>()
    val INVALID_REFINES_IN_SWIFT_TARGETS by error0<KtElement>()
    val SUBTYPE_OF_HIDDEN_FROM_OBJC by error0<KtElement>()
    val CANNOT_CHECK_FOR_FORWARD_DECLARATION by error1<KtElement, ConeKotlinType>()
    val UNCHECKED_CAST_TO_FORWARD_DECLARATION by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT by error1<KtElement, ConeKotlinType>()
    val FORWARD_DECLARATION_AS_CLASS_LITERAL by error1<KtElement, ConeKotlinType>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirNativeErrorsDefaultMessages)
    }
}
