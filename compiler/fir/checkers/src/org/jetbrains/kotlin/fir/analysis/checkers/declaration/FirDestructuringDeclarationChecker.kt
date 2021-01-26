/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.extended.report
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

object FirDestructuringDeclarationChecker : FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        // val (...) = `destructuring_declaration`
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
            assert(declaration.name.isSpecial && declaration.name.asString() == "<destruct>") {
                "Unexpected name: ${declaration.name.asString()} for destructuring declaration"
            }
            checkInitializer(source, declaration.initializer, reporter)
            return
        }

        // val (`destructuring_declaration_entry`, ...) = ...
        if (source.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) return

        val componentCall = declaration.initializer as? FirComponentCall ?: return
        val reference = componentCall.calleeReference as? FirErrorNamedReference ?: return

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
                is FirVariable<*> -> originalDestructuringDeclarationOrInitializer.returnTypeRef
                is FirExpression -> originalDestructuringDeclarationOrInitializer.typeRef
                else -> null
            } ?: return

        when (val diagnostic = reference.diagnostic) {
            is ConeUnresolvedNameError -> {
                reporter.report(
                    FirErrors.COMPONENT_FUNCTION_MISSING.on(
                        originalDestructuringDeclarationOrInitializerSource,
                        diagnostic.name,
                        originalDestructuringDeclarationType
                    )
                )
            }
            is ConeAmbiguityError -> {
                reporter.report(
                    FirErrors.COMPONENT_FUNCTION_AMBIGUITY.on(
                        originalDestructuringDeclarationOrInitializerSource,
                        diagnostic.name,
                        diagnostic.candidates
                    )
                )
            }
            // TODO: COMPONENT_FUNCTION_ON_NULLABLE
            // TODO: COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH
        }
    }

    private fun checkInitializer(source: FirSourceElement, initializer: FirExpression?, reporter: DiagnosticReporter) {
        val needToReport =
            when (initializer) {
                null -> true
                is FirErrorExpression -> (initializer.diagnostic as? ConeSimpleDiagnostic)?.kind == DiagnosticKind.Syntax
                else -> false
            }
        if (needToReport) {
            reporter.report(source, FirErrors.INITIALIZER_REQUIRED_FOR_DESTRUCTURING_DECLARATION)
        }
    }

    private val FirExpression.explicitReceiverOfQualifiedAccess: FirQualifiedAccessExpression?
        get() = (this as? FirQualifiedAccess)?.explicitReceiver?.unwrapped as? FirQualifiedAccessExpression

    private val FirExpression.unwrapped: FirExpression
        get() =
            when (this) {
                is FirExpressionWithSmartcast -> this.originalExpression
                is FirWrappedExpression -> this.expression
                else -> this
            }

    private val FirQualifiedAccessExpression.resolvedVariable: FirVariable<*>?
        get() = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirVariableSymbol)?.fir as? FirVariable<*>
}
