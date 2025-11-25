/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaredMemberScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.analysis.diagnostics.toInvisibleReferenceDiagnostic
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addToStdlib.runIf

object FirDestructuringDeclarationChecker : FirPropertyChecker(MppCheckerKind.Common) {
    private enum class DestructuringSyntax {
        ParensShort,
        ParensFull,
        SquareBracketsShort,
        SquareBracketsFull,
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val source = declaration.source ?: return
        // val (...) = `destructuring_declaration`
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
            checkInitializer(source, declaration.initializer)
            checkSquareBracketsLanguageFeature(source)
            return
        }

        if (declaration.name == SpecialNames.DESTRUCT) {
            checkSquareBracketsLanguageFeature(source)
        }

        // val (`destructuring_declaration_entry`, ...) = ...
        if (source.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) return

        val initializer = declaration.initializer as? FirQualifiedAccessExpression ?: return
        val originalDestructuringDeclaration = getDestructuringVariableOfEntry(declaration) ?: return
        val originalDestructuringDeclarationOrInitializer =
            when (originalDestructuringDeclaration) {
                is FirProperty -> {
                    if (originalDestructuringDeclaration.initializer?.source?.elementType == KtNodeTypes.FOR) {
                        // for ((entry, ...) = `destructuring_declaration`) { ... }
                        // It will be wrapped as `next()` call whose explicit receiver is `iterator()` on the actual source.
                        val iterator = originalDestructuringDeclaration.initializer?.explicitReceiverOfQualifiedAccess
                        (iterator?.resolvedVariable as? FirProperty)?.initializer?.explicitReceiverOfQualifiedAccess
                    } else {
                        // val (entry, ...) = `destructuring_declaration`
                        originalDestructuringDeclaration.initializer
                    }
                }
                is FirValueParameter -> {
                    // ... = { `(entry, ...)` -> ... } // value parameter itself is a destructuring declaration
                    originalDestructuringDeclaration
                }
                else -> null
            } ?: return

        val syntax = source.syntaxKind(originalDestructuringDeclaration)

        if (syntax == DestructuringSyntax.ParensFull) {
            checkFullFormLanguageFeature(source)
        }

        if (initializer !is FirComponentCall) return

        // We check above that initializer is a component call, therefore, we must have positional destructuring.
        source.getChild(KtTokens.EQ, depth = 1)?.let {
            reporter.reportOn(
                it,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.EnableNameBasedDestructuringShortForm to context.languageVersionSettings
            )
        }

        if (originalDestructuringDeclarationOrInitializer.isMissingInitializer()) return
        val originalDestructuringDeclarationOrInitializerSource = originalDestructuringDeclarationOrInitializer.source ?: return
        val originalDestructuringDeclarationType =
            when (originalDestructuringDeclarationOrInitializer) {
                is FirVariable -> originalDestructuringDeclarationOrInitializer.returnTypeRef.coneType
                is FirExpression -> originalDestructuringDeclarationOrInitializer.resolvedType
                else -> null
            } ?: return

        val reference = initializer.calleeReference
        val diagnostic = if (reference.isError()) reference.diagnostic else null
        if (diagnostic != null) {
            reportGivenDiagnostic(
                originalDestructuringDeclarationOrInitializerSource,
                originalDestructuringDeclarationType,
                diagnostic,
                declaration,
                initializer,
            )
        }

        checkComponentTypeMismatch(
            originalDestructuringDeclarationOrInitializerSource,
            declaration,
            originalDestructuringDeclaration,
            initializer
        )

        if (syntax == DestructuringSyntax.ParensShort) {
            checkChangingMeaningOfShortSyntax(declaration, originalDestructuringDeclarationType, initializer.componentIndex, source)
        }
    }

    fun getDestructuringVariableIfEntry(declaration: FirProperty): FirVariableSymbol<*>? {
        return runIf(declaration.source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) {
            getDestructuringVariableOfEntry(declaration)?.symbol
        }
    }

    private fun getDestructuringVariableOfEntry(declaration: FirProperty): FirVariable? {
        return declaration.initializer?.explicitReceiverOfQualifiedAccess?.resolvedVariable
    }

    private fun KtSourceElement.syntaxKind(originalDestructuringDeclaration: FirVariable): DestructuringSyntax {
        val hasOpeningSquareBracket = originalDestructuringDeclaration.source?.findSquareBracket() != null
        val hasValVar = getChild(KtTokens.VAL_VAR, depth = 1) != null

        return when (hasOpeningSquareBracket) {
            true -> when (hasValVar) {
                true -> DestructuringSyntax.SquareBracketsFull
                false -> DestructuringSyntax.SquareBracketsShort
            }
            false -> when (hasValVar) {
                true -> DestructuringSyntax.ParensFull
                false -> DestructuringSyntax.ParensShort
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFullFormLanguageFeature(source: KtSourceElement) {
        if (LanguageFeature.NameBasedDestructuring.isEnabled()) return

        source.getChild(KtTokens.VAL_VAR, depth = 1)?.let {
            reporter.reportOn(
                it,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.NameBasedDestructuring to context.languageVersionSettings
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkSquareBracketsLanguageFeature(source: KtSourceElement) {
        if (LanguageFeature.NameBasedDestructuring.isEnabled()) return

        val lBracket = source.findSquareBracket()
        if (lBracket != null) {
            reporter.reportOn(
                lBracket,
                FirErrors.UNSUPPORTED_FEATURE,
                LanguageFeature.NameBasedDestructuring to context.languageVersionSettings
            )
        }
    }

    private fun KtSourceElement.findSquareBracket(): KtSourceElement? {
        return when (elementType) {
            KtNodeTypes.DESTRUCTURING_DECLARATION -> getChild(KtTokens.LBRACKET, depth = 1)
            KtNodeTypes.VALUE_PARAMETER -> getChild(KtNodeTypes.DESTRUCTURING_DECLARATION, depth = 1)?.findSquareBracket()
            else -> null
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkChangingMeaningOfShortSyntax(
        declaration: FirProperty,
        originalDestructuringDeclarationType: ConeKotlinType,
        componentIndex: Int,
        source: KtSourceElement,
    ) {
        if (!LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses.isEnabled()
            || LanguageFeature.EnableNameBasedDestructuringShortForm.isEnabled()
        ) {
            return
        }

        if (declaration.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
            reporter.reportOn(source, FirErrors.DESTRUCTURING_SHORT_FORM_UNDERSCORE)
            return
        }

        val problem = originalDestructuringDeclarationType.getProblem(componentIndex, declaration.name) ?: return
        when (problem) {
            NonDataClass, DataClassCustomComponent -> reporter.reportOn(
                source,
                FirErrors.DESTRUCTURING_SHORT_FORM_OF_NON_DATA_CLASS,
                originalDestructuringDeclarationType,
                declaration.name,
                if (problem is NonDataClass) "non-data class" else "custom component operators of data class",
            )
            is DataClassNameMismatch -> reporter.reportOn(
                source,
                FirErrors.DESTRUCTURING_SHORT_FORM_NAME_MISMATCH,
                declaration.name,
                problem.propertyName,
            )
        }
    }

    private sealed class ProblemType
    private class DataClassNameMismatch(val propertyName: Name) : ProblemType()
    private object NonDataClass : ProblemType()
    private object DataClassCustomComponent : ProblemType()

    context(context: CheckerContext)
    private fun ConeKotlinType.getProblem(componentIndex: Int, destructuredName: Name): ProblemType? {
        val classSymbol = fullyExpandedType().toRegularClassSymbol() ?: return null
        val propertyName = when {
            classSymbol.isData -> {
                val constructor = classSymbol.declaredMemberScope().getDeclaredConstructors().firstOrNull { it.isPrimary }
                constructor?.valueParameterSymbols?.elementAtOrNull(componentIndex - 1)?.name
            }
            classSymbol.classId == StandardClassIds.MapEntry -> when (componentIndex) {
                1 -> StandardNames.MAP_ENTRY_KEY
                2 -> StandardNames.MAP_ENTRY_VALUE
                else -> null
            }
            else -> null
        }

        return when {
            // If this condition is true for the first entry, it is true for all entries.
            // Suppress repeated diagnostics by only reporting on the first one.
            !classSymbol.isData && propertyName == null && componentIndex == 1 -> NonDataClass
            classSymbol.isData && propertyName == null -> DataClassCustomComponent
            propertyName != null && propertyName != destructuredName -> DataClassNameMismatch(propertyName)
            else -> null
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkInitializer(
        source: KtSourceElement,
        initializer: FirExpression?,
    ) {
        if (initializer.isMissingInitializer()) {
            reporter.reportOn(source, FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION)
        }
    }

    private fun FirElement?.isMissingInitializer(): Boolean {
        return this == null || this is FirErrorExpression && diagnostic is ConeSyntaxDiagnostic
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    @OptIn(ApplicabilityDetail::class)
    private fun reportGivenDiagnostic(
        source: KtSourceElement,
        destructuringDeclarationType: ConeKotlinType,
        diagnostic: ConeDiagnostic,
        property: FirProperty,
        componentCall: FirComponentCall,
    ) {
        when (diagnostic) {
            is ConeUnresolvedNameError -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.name,
                    destructuringDeclarationType
                )
            }
            is ConeHiddenCandidateError -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.candidate.callInfo.name,
                    destructuringDeclarationType
                )
            }
            is ConeInapplicableWrongReceiver -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.candidates.first().callInfo.name,
                    destructuringDeclarationType
                )
            }
            is ConeAmbiguityError if diagnostic.applicability.isSuccess -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_AMBIGUITY,
                    diagnostic.name,
                    diagnostic.candidates.map { it.symbol },
                    destructuringDeclarationType,
                )
            }
            is ConeAmbiguityError -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.name,
                    destructuringDeclarationType
                )
            }
            is ConeInapplicableCandidateError -> {
                if (destructuringDeclarationType.fullyExpandedType().isMarkedNullable) {
                    reporter.reportOn(
                        source,
                        FirErrors.COMPONENT_FUNCTION_ON_NULLABLE,
                        (diagnostic.candidate.symbol as FirNamedFunctionSymbol).callableId.callableName,
                        destructuringDeclarationType
                    )
                } else {
                    reportDefaultDiagnostics(diagnostic, componentCall)
                }
            }
            is ConeConstraintSystemHasContradiction -> {
                val componentType = componentCall.resolvedType
                if (componentType is ConeErrorType) {
                    reporter.reportOn(
                        source,
                        FirErrors.COMPONENT_FUNCTION_MISSING,
                        diagnostic.candidates.first().callInfo.name,
                        destructuringDeclarationType
                    )
                    return
                }
            }
            is ConeVisibilityError -> {
                reporter.report(diagnostic.symbol.toInvisibleReferenceDiagnostic(property.source, context.session), context)
            }
            else -> {
                reportDefaultDiagnostics(diagnostic, componentCall)
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun checkComponentTypeMismatch(
        source: KtSourceElement,
        property: FirProperty,
        destructuringDeclaration: FirVariable,
        componentCall: FirComponentCall,
    ) {
        val componentType = componentCall.resolvedType

        val expectedType = property.returnTypeRef.coneType
        if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, componentType, expectedType)) {
            val typeMismatchSource =
                // ... = { `(entry, ...)` -> ... } // Report on specific `entry`
                if (destructuringDeclaration is FirValueParameter)
                    property.source
                // val (entry, ...) = `destructuring_declaration` // Report on a destructuring declaration
                else
                    source
            reporter.reportOn(
                typeMismatchSource,
                FirErrors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH,
                componentCall.calleeReference.name,
                componentType,
                expectedType
            )
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportDefaultDiagnostics(
        diagnostic: ConeDiagnostic,
        componentCall: FirComponentCall,
    ) {
        for (coneDiagnostic in diagnostic.toFirDiagnostics(context.session, componentCall.source, null)) {
            reporter.report(coneDiagnostic, context)
        }
    }

    private val FirExpression.explicitReceiverOfQualifiedAccess: FirQualifiedAccessExpression?
        get() = (this as? FirQualifiedAccessExpression)?.explicitReceiver?.unwrapped as? FirQualifiedAccessExpression

    private val FirExpression.unwrapped: FirExpression
        get() =
            when (this) {
                is FirSmartCastExpression -> this.originalExpression
                is FirWrappedExpression -> this.expression
                else -> this
            }

    private val FirQualifiedAccessExpression.resolvedVariable: FirVariable?
        get() {
            val symbol = calleeReference.toResolvedVariableSymbol() ?: return null
            symbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            @OptIn(SymbolInternals::class)
            return symbol.fir
        }
}
