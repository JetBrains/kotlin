/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticList
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.PositioningStrategy
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

@Suppress("UNUSED_VARIABLE", "LocalVariableName", "ClassName", "unused")
@OptIn(PrivateForInline::class)
object NATIVE_DIAGNOSTICS_LIST : DiagnosticList("FirNativeErrors") {
    val ALL by object : DiagnosticGroup("All") {
        val THROWS_LIST_EMPTY by error<KtElement>()
        val INCOMPATIBLE_THROWS_OVERRIDE by error<KtElement> {
            parameter<FirRegularClassSymbol>("containingClass")
        }
        val INCOMPATIBLE_THROWS_INHERITED by error<KtDeclaration> {
            parameter<Collection<FirRegularClassSymbol>>("containingClasses")
        }
        val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND by error<KtElement> {
            parameter<FqName>("exceptionName")
        }
        val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY by error<KtElement>()
        val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL by error<KtElement>()
        val INAPPLICABLE_THREAD_LOCAL by error<KtElement>()
        val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL by error<KtElement>()
        val INVALID_CHARACTERS_NATIVE by deprecationError<PsiElement>(
            LanguageFeature.ProhibitInvalidCharsInNativeIdentifiers,
            PositioningStrategy.NAME_IDENTIFIER
        ) {
            parameter<String>("message")
        }
        val REDUNDANT_SWIFT_REFINEMENT by error<KtElement>()
        val INCOMPATIBLE_OBJC_REFINEMENT_OVERRIDE by error<KtElement> {
            parameter<FirBasedSymbol<*>>("declaration")
            parameter<Collection<FirRegularClassSymbol>>("containingClasses")
        }
        val INAPPLICABLE_OBJC_NAME by error<KtElement>()
        val INVALID_OBJC_NAME by error<KtElement>()
        val INVALID_OBJC_NAME_CHARS by error<KtElement> {
            parameter<String>("characters")
        }
        val INVALID_OBJC_NAME_FIRST_CHAR by error<KtElement> {
            parameter<String>("characters")
        }
        val EMPTY_OBJC_NAME by error<KtElement>()
        val INCOMPATIBLE_OBJC_NAME_OVERRIDE by error<KtElement> {
            parameter<FirBasedSymbol<*>>("declaration")
            parameter<Collection<FirRegularClassSymbol>>("containingClasses")
        }
        val INAPPLICABLE_EXACT_OBJC_NAME by error<KtElement>()
        val MISSING_EXACT_OBJC_NAME by error<KtElement>()
        val NON_LITERAL_OBJC_NAME_ARG by error<KtElement>()
        val INVALID_OBJC_HIDES_TARGETS by error<KtElement>()
        val INVALID_REFINES_IN_SWIFT_TARGETS by error<KtElement>()
        val SUBTYPE_OF_HIDDEN_FROM_OBJC by error<KtElement>()

        val CANNOT_CHECK_FOR_FORWARD_DECLARATION by error<KtElement>() {
            parameter<ConeKotlinType>("type")
        }
        val UNCHECKED_CAST_TO_FORWARD_DECLARATION by warning<KtElement> {
            parameter<ConeKotlinType>("sourceType")
            parameter<ConeKotlinType>("destinationType")
        }
        val FORWARD_DECLARATION_AS_REIFIED_TYPE_ARGUMENT by error<KtElement> {
            parameter<ConeKotlinType>("type")
        }
        val FORWARD_DECLARATION_AS_CLASS_LITERAL by error<KtElement> {
            parameter<ConeKotlinType>("type")
        }
        val TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE by error<KtElement>()
        val PROPERTY_MUST_BE_VAR by error<KtElement>() {
            parameter<FqName>("annotationName")
        }
        val MUST_NOT_HAVE_EXTENSION_RECEIVER by error<KtElement> {
            parameter<String>("annotationKind")
        }
        val MUST_BE_OBJC_OBJECT_TYPE by error<KtElement>() {
            parameter<String>("annotationName")
            parameter<ConeKotlinType>("unexpectedType")
        }
        val MUST_BE_UNIT_TYPE by error<KtElement>() {
            parameter<String>("annotationKind")
            parameter<ConeKotlinType>("unexpectedType")
        }
        val CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER by error<KtElement>() {
            parameter<FqName>("annotation")
        }
        val CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR by error<KtElement>() {
            parameter<FqName>("annotation")
        }
        val CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS by error<KtElement>() {
            parameter<FqName>("annotation")
        }
        val CONFLICTING_OBJC_OVERLOADS by error<PsiElement>() {
            parameter<Collection<Symbol>>("conflictingOverloads")
        }
        val INAPPLICABLE_OBJC_OVERRIDE by error<PsiElement>()
    }
}
