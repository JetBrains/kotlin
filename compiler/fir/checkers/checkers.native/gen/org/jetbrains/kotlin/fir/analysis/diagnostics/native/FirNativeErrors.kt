/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature.ProhibitInvalidCharsInNativeIdentifiers
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryForDeprecation1
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.NATIVE_DIAGNOSTICS_LIST]
 */
object FirNativeErrors {
    // All
    val THROWS_LIST_EMPTY: KtDiagnosticFactory0 by error0<KtElement>()
    val INCOMPATIBLE_THROWS_OVERRIDE: KtDiagnosticFactory1<FirRegularClassSymbol> by error1<KtElement, FirRegularClassSymbol>()
    val INCOMPATIBLE_THROWS_INHERITED: KtDiagnosticFactory1<Collection<FirRegularClassSymbol>> by error1<KtDeclaration, Collection<FirRegularClassSymbol>>()
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND: KtDiagnosticFactory1<FqName> by error1<KtElement, FqName>()
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY: KtDiagnosticFactory0 by error0<KtElement>()
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL: KtDiagnosticFactory0 by error0<KtElement>()
    val INAPPLICABLE_THREAD_LOCAL: KtDiagnosticFactory0 by error0<KtElement>()
    val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_CHARACTERS_NATIVE: KtDiagnosticFactoryForDeprecation1<String> by deprecationError1<PsiElement, String>(ProhibitInvalidCharsInNativeIdentifiers, SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val REDUNDANT_SWIFT_REFINEMENT: KtDiagnosticFactory0 by error0<KtElement>()
    val INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> by error2<KtElement, FirBasedSymbol<*>, Collection<FirRegularClassSymbol>>()
    val INAPPLICABLE_OBJC_NAME: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_OBJC_NAME: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_OBJC_NAME_CHARS: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val INVALID_OBJC_NAME_FIRST_CHAR: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val EMPTY_OBJC_NAME: KtDiagnosticFactory0 by error0<KtElement>()
    val INCOMPATIBLE_OBJC_NAME_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> by error2<KtElement, FirBasedSymbol<*>, Collection<FirRegularClassSymbol>>()
    val INAPPLICABLE_EXACT_OBJC_NAME: KtDiagnosticFactory0 by error0<KtElement>()
    val MISSING_EXACT_OBJC_NAME: KtDiagnosticFactory0 by error0<KtElement>()
    val NON_LITERAL_OBJC_NAME_ARG: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_OBJC_HIDES_TARGETS: KtDiagnosticFactory0 by error0<KtElement>()
    val INVALID_REFINES_IN_SWIFT_TARGETS: KtDiagnosticFactory0 by error0<KtElement>()
    val SUBTYPE_OF_HIDDEN_FROM_OBJC: KtDiagnosticFactory0 by error0<KtElement>()
    val CANNOT_CHECK_FOR_FORWARD_DECLARATION: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>()
    val UNCHECKED_CAST_TO_FORWARD_DECLARATION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> by warning2<KtElement, ConeKotlinType, ConeKotlinType>()
    val FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>()
    val FORWARD_DECLARATION_AS_CLASS_LITERAL: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>()
    val TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE: KtDiagnosticFactory0 by error0<KtElement>()
    val PROPERTY_MUST_BE_VAR: KtDiagnosticFactory1<FqName> by error1<KtElement, FqName>()
    val MUST_NOT_HAVE_EXTENSION_RECEIVER: KtDiagnosticFactory1<String> by error1<KtElement, String>()
    val MUST_BE_OBJC_OBJECT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> by error2<KtElement, String, ConeKotlinType>()
    val MUST_BE_UNIT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> by error2<KtElement, String, ConeKotlinType>()
    val CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER: KtDiagnosticFactory1<FqName> by error1<KtElement, FqName>()
    val CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR: KtDiagnosticFactory1<FqName> by error1<KtElement, FqName>()
    val CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS: KtDiagnosticFactory1<FqName> by error1<KtElement, FqName>()
    val CONFLICTING_OBJC_OVERLOADS: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> by error1<PsiElement, Collection<FirBasedSymbol<*>>>()
    val INAPPLICABLE_OBJC_OVERRIDE: KtDiagnosticFactory0 by error0<PsiElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirNativeErrorsDefaultMessages)
    }
}
