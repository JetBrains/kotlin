/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSyntaxDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.isError
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirDestructuringDeclarationChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        // val (...) = `destructuring_declaration`
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
            checkInitializer(source, declaration.initializer, reporter, context)
            return
        }

        // val (`destructuring_declaration_entry`, ...) = ...
        if (source.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) return

        val componentCall = declaration.initializer as? FirComponentCall ?: return
        val originalExpression = componentCall.explicitReceiverOfQualifiedAccess ?: return
        val originalDestructuringDeclaration = originalExpression.resolvedVariable ?: return
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
        val originalDestructuringDeclarationOrInitializerSource = originalDestructuringDeclarationOrInitializer.source ?: return
        val originalDestructuringDeclarationType =
            when (originalDestructuringDeclarationOrInitializer) {
                is FirVariable -> originalDestructuringDeclarationOrInitializer.returnTypeRef.coneType
                is FirExpression -> originalDestructuringDeclarationOrInitializer.typeRef.coneType
                else -> null
            } ?: return

        val reference = componentCall.calleeReference
        val diagnostic = if (reference.isError()) reference.diagnostic else null
        if (diagnostic != null) {
            checkComponentCall(
                originalDestructuringDeclarationOrInitializerSource,
                originalDestructuringDeclarationType,
                diagnostic,
                declaration,
                componentCall,
                originalDestructuringDeclaration,
                reporter,
                context
            )
        }
    }

    private fun checkInitializer(
        source: KtSourceElement,
        initializer: FirExpression?,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        val needToReport =
            when (initializer) {
                null -> true
                is FirErrorExpression -> initializer.diagnostic is ConeSyntaxDiagnostic
                else -> false
            }
        if (needToReport) {
            reporter.reportOn(source, FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION, context)
        }
    }

    private fun checkComponentCall(
        source: KtSourceElement,
        destructuringDeclarationType: ConeKotlinType,
        diagnostic: ConeDiagnostic,
        property: FirProperty,
        componentCall: FirComponentCall,
        destructuringDeclaration: FirVariable,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        when (diagnostic) {
            is ConeUnresolvedNameError -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.name,
                    destructuringDeclarationType,
                    context
                )
            }
            is ConeInapplicableWrongReceiver -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_MISSING,
                    diagnostic.candidates.first().callInfo.name,
                    destructuringDeclarationType,
                    context
                )
            }
            is ConeAmbiguityError -> {
                reporter.reportOn(
                    source,
                    FirErrors.COMPONENT_FUNCTION_AMBIGUITY,
                    diagnostic.name,
                    diagnostic.candidates.map { it.symbol },
                    context
                )
            }
            is ConeInapplicableCandidateError -> {
                if (destructuringDeclarationType.isNullable) {
                    reporter.reportOn(
                        source,
                        FirErrors.COMPONENT_FUNCTION_ON_NULLABLE,
                        (diagnostic.candidate.symbol as FirNamedFunctionSymbol).callableId.callableName,
                        context
                    )
                }
            }
            is ConeConstraintSystemHasContradiction -> {
                val componentType = componentCall.typeRef.coneType
                if (componentType is ConeErrorType) {
                    // There will be other errors on this error type.
                    return
                }
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
                        diagnostic.candidate.callInfo.name,
                        componentType,
                        expectedType,
                        context
                    )
                }
            }
            is ConeVisibilityError -> {
                reporter.reportOn(
                    property.source,
                    FirErrors.INVISIBLE_REFERENCE,
                    diagnostic.symbol,
                    context,
                )
            }
            else -> error("Unhandled error during a component function call")
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
