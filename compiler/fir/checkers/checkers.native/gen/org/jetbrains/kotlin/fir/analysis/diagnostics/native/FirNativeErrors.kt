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
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
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
    val THROWS_LIST_EMPTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("THROWS_LIST_EMPTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INCOMPATIBLE_THROWS_OVERRIDE: KtDiagnosticFactory1<FirRegularClassSymbol> = KtDiagnosticFactory1("INCOMPATIBLE_THROWS_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INCOMPATIBLE_THROWS_INHERITED: KtDiagnosticFactory1<Collection<FirRegularClassSymbol>> = KtDiagnosticFactory1("INCOMPATIBLE_THROWS_INHERITED", ERROR, SourceElementPositioningStrategies.DEFAULT, KtDeclaration::class)
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_THREAD_LOCAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_THREAD_LOCAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_CHARACTERS_NATIVE: KtDiagnosticFactoryForDeprecation1<String> = KtDiagnosticFactoryForDeprecation1("INVALID_CHARACTERS_NATIVE", ProhibitInvalidCharsInNativeIdentifiers, SourceElementPositioningStrategies.NAME_IDENTIFIER, PsiElement::class)
    val REDUNDANT_SWIFT_REFINEMENT: KtDiagnosticFactory0 = KtDiagnosticFactory0("REDUNDANT_SWIFT_REFINEMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> = KtDiagnosticFactory2("INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_OBJC_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_OBJC_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_OBJC_NAME_CHARS: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_OBJC_NAME_CHARS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_OBJC_NAME_FIRST_CHAR: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("INVALID_OBJC_NAME_FIRST_CHAR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val EMPTY_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("EMPTY_OBJC_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INCOMPATIBLE_OBJC_NAME_OVERRIDE: KtDiagnosticFactory2<FirBasedSymbol<*>, Collection<FirRegularClassSymbol>> = KtDiagnosticFactory2("INCOMPATIBLE_OBJC_NAME_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INAPPLICABLE_EXACT_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_EXACT_OBJC_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val MISSING_EXACT_OBJC_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("MISSING_EXACT_OBJC_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val NON_LITERAL_OBJC_NAME_ARG: KtDiagnosticFactory0 = KtDiagnosticFactory0("NON_LITERAL_OBJC_NAME_ARG", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_OBJC_HIDES_TARGETS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_OBJC_HIDES_TARGETS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val INVALID_REFINES_IN_SWIFT_TARGETS: KtDiagnosticFactory0 = KtDiagnosticFactory0("INVALID_REFINES_IN_SWIFT_TARGETS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val SUBTYPE_OF_HIDDEN_FROM_OBJC: KtDiagnosticFactory0 = KtDiagnosticFactory0("SUBTYPE_OF_HIDDEN_FROM_OBJC", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CANNOT_CHECK_FOR_FORWARD_DECLARATION: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("CANNOT_CHECK_FOR_FORWARD_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val UNCHECKED_CAST_TO_FORWARD_DECLARATION: KtDiagnosticFactory2<ConeKotlinType, ConeKotlinType> = KtDiagnosticFactory2("UNCHECKED_CAST_TO_FORWARD_DECLARATION", WARNING, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val FORWARD_DECLARATION_AS_CLASS_LITERAL: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("FORWARD_DECLARATION_AS_CLASS_LITERAL", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE: KtDiagnosticFactory0 = KtDiagnosticFactory0("TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val PROPERTY_MUST_BE_VAR: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("PROPERTY_MUST_BE_VAR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val MUST_NOT_HAVE_EXTENSION_RECEIVER: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("MUST_NOT_HAVE_EXTENSION_RECEIVER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val MUST_BE_OBJC_OBJECT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("MUST_BE_OBJC_OBJECT_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val MUST_BE_UNIT_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> = KtDiagnosticFactory2("MUST_BE_UNIT_TYPE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS: KtDiagnosticFactory1<FqName> = KtDiagnosticFactory1("CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val CONFLICTING_OBJC_OVERLOADS: KtDiagnosticFactory1<Collection<FirBasedSymbol<*>>> = KtDiagnosticFactory1("CONFLICTING_OBJC_OVERLOADS", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val INAPPLICABLE_OBJC_OVERRIDE: KtDiagnosticFactory0 = KtDiagnosticFactory0("INAPPLICABLE_OBJC_OVERRIDE", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirNativeErrorsDefaultMessages)
    }
}
