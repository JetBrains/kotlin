/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.analysis.diagnostics.toInvisibleReferenceDiagnostic
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
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirDestructuringDeclarationChecker : FirPropertyChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val source = declaration.source ?: return
        // val (...) = `destructuring_declaration`
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
            checkInitializer(source, declaration.initializer)
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
        if (originalDestructuringDeclarationOrInitializer.isMissingInitializer()) return
        val originalDestructuringDeclarationOrInitializerSource = originalDestructuringDeclarationOrInitializer.source ?: return
        val originalDestructuringDeclarationType =
            when (originalDestructuringDeclarationOrInitializer) {
                is FirVariable -> originalDestructuringDeclarationOrInitializer.returnTypeRef.coneType
                is FirExpression -> originalDestructuringDeclarationOrInitializer.resolvedType
                else -> null
            } ?: return

        val reference = componentCall.calleeReference
        val diagnostic = if (reference.isError()) reference.diagnostic else null
        if (diagnostic != null) {
            reportGivenDiagnostic(
                originalDestructuringDeclarationOrInitializerSource,
                originalDestructuringDeclarationType,
                diagnostic,
                declaration,
                componentCall,
            )
        }

        checkComponentTypeMismatch(
            originalDestructuringDeclarationOrInitializerSource,
            declaration,
            originalDestructuringDeclaration,
            componentCall
        )
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
