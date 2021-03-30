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
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

class ErrorNodeDiagnosticCollectorComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source ?: return
        reportFirDiagnostic(errorLoop.diagnostic, source, reporter, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        val source = errorTypeRef.source ?: return
        reportFirDiagnostic(errorTypeRef.diagnostic, source, reporter, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: CheckerContext) {
        val errorType = resolvedTypeRef.type as? ConeClassErrorType ?: return
        val source = resolvedTypeRef.source ?: return
        reportFirDiagnostic(errorType.diagnostic, source, reporter, data)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        val source = errorNamedReference.source ?: return
        val qualifiedAccess = data.qualifiedAccesses.lastOrNull()?.takeIf {
            // Use the source of the enclosing FirQualifiedAccessExpression if it is exactly the call to the erroneous callee.
            it is FirQualifiedAccessExpression && it.calleeReference == errorNamedReference
        }
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source.elementType == KtNodeTypes.ANNOTATION_ENTRY && errorNamedReference.diagnostic is ConeUnresolvedNameError) return
        // Already reported in FirConventionFunctionCallChecker
        if (source.kind == FirFakeSourceElementKind.ArrayAccessNameReference &&
            errorNamedReference.diagnostic is ConeUnresolvedNameError
        ) return

        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        if (qualifiedAccess?.dispatchReceiver.hasUnresolvedNameError() || qualifiedAccess?.extensionReceiver.hasUnresolvedNameError() || qualifiedAccess?.explicitReceiver.hasUnresolvedNameError()) return

        reportFirDiagnostic(errorNamedReference.diagnostic, source, reporter, data, qualifiedAccess?.source)
    }

    private fun FirExpression?.hasUnresolvedNameError(): Boolean {
        return when ((this?.typeRef as? FirErrorTypeRef)?.diagnostic) {
            is ConeUnresolvedNameError -> true
            else -> false
        }
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
        context: CheckerContext,
        qualifiedAccessSource: FirSourceElement? = null
    ) {
        // Will be handled by [FirDestructuringDeclarationChecker]
        if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) {
            return
        }
        if (source.kind == FirFakeSourceElementKind.ImplicitConstructor) {
            return
        }
        for (coneDiagnostic in diagnostic.toFirDiagnostics(source, qualifiedAccessSource)) {
            reporter.report(coneDiagnostic, context)
        }
    }
}
