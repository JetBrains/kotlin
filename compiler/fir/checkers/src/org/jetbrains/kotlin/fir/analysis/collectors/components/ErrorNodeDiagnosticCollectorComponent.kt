/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef

class ErrorNodeDiagnosticCollectorComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source ?: return
        reportFirDiagnostic(errorLoop.diagnostic, source, reporter, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        val source = errorTypeRef.source ?: return
        reportFirDiagnostic(errorTypeRef.diagnostic, source, reporter, data)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        val source = data.qualifiedAccesses.lastOrNull()?.source?.takeIf { it.elementType == KtNodeTypes.DOT_QUALIFIED_EXPRESSION }
            ?: errorNamedReference.source ?: return
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source.elementType == KtNodeTypes.ANNOTATION_ENTRY && errorNamedReference.diagnostic is ConeUnresolvedNameError) return
        reportFirDiagnostic(errorNamedReference.diagnostic, source, reporter, data)
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: CheckerContext) {
        val source = errorExpression.source ?: return
        reportFirDiagnostic(errorExpression.diagnostic, source, reporter, data)
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: CheckerContext) {
        val source = errorFunction.source ?: return
        reportFirDiagnostic(errorFunction.diagnostic, source, reporter, data)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: CheckerContext) {
        val source = errorResolvedQualifier.source ?: return
        reportFirDiagnostic(errorResolvedQualifier.diagnostic, source, reporter, data)
    }

    private fun reportFirDiagnostic(
        diagnostic: ConeDiagnostic,
        source: FirSourceElement,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        // Will be handled by [FirDestructuringDeclarationChecker]
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) {
            // TODO: if all diagnostics are supported, we don't need the following check, and will bail out based on element type.
            if (diagnostic is ConeUnresolvedNameError ||
                diagnostic is ConeAmbiguityError ||
                diagnostic is ConeInapplicableCandidateError
            ) {
                return
            }
        }

        if (source.kind == FirFakeSourceElementKind.ImplicitConstructor) {
            return
        }
        val coneDiagnostic = diagnostic.toFirDiagnostic(source)
        reporter.report(coneDiagnostic, context)
    }
}
