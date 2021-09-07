/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.render

class ErrorNodeDiagnosticCollectorComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source ?: return
        reportFirDiagnostic(errorLoop.diagnostic, source, reporter, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        val source = errorTypeRef.source ?: return
        reportFirDiagnostic(errorTypeRef.diagnostic, source, reporter, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: CheckerContext) {
        assert(resolvedTypeRef.type !is ConeKotlinErrorType) {
            "Instead use FirErrorTypeRef for ${resolvedTypeRef.type.render()}"
        }
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        val source = errorNamedReference.source ?: return
        val qualifiedAccessOrAnnotationCall = data.qualifiedAccessOrAnnotationCalls.lastOrNull()?.takeIf {
            // Use the source of the enclosing FirQualifiedAccess if it is exactly the call to the erroneous callee.
            when (it) {
                is FirQualifiedAccess -> it.calleeReference == errorNamedReference
                is FirAnnotationCall -> it.calleeReference == errorNamedReference
                else -> false
            }
        }
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source.elementType == KtNodeTypes.ANNOTATION_ENTRY && errorNamedReference.diagnostic is ConeUnresolvedNameError) return
        // Already reported in FirConventionFunctionCallChecker
        if (source.kind == FirFakeSourceElementKind.ArrayAccessNameReference &&
            errorNamedReference.diagnostic is ConeUnresolvedNameError
        ) return

        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        if (qualifiedAccessOrAnnotationCall is FirQualifiedAccess) {
            if (qualifiedAccessOrAnnotationCall.dispatchReceiver.cannotBeResolved() ||
                qualifiedAccessOrAnnotationCall.extensionReceiver.cannotBeResolved() ||
                qualifiedAccessOrAnnotationCall.explicitReceiver.cannotBeResolved()
            ) return
        }

        reportFirDiagnostic(errorNamedReference.diagnostic, source, reporter, data, qualifiedAccessOrAnnotationCall?.source)
    }

    private fun FirExpression?.cannotBeResolved(): Boolean {
        return when (val diagnostic = (this?.typeRef as? FirErrorTypeRef)?.diagnostic) {
            is ConeUnresolvedNameError, is ConeInstanceAccessBeforeSuperCall -> true
            is ConeSimpleDiagnostic -> diagnostic.kind == DiagnosticKind.NotASupertype ||
                    diagnostic.kind == DiagnosticKind.SuperNotAvailable ||
                    diagnostic.kind == DiagnosticKind.UnresolvedLabel ||
                    diagnostic.kind == DiagnosticKind.AmbiguousSuper
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

    override fun visitErrorImport(errorImport: FirErrorImport, data: CheckerContext) {
        val source = errorImport.source ?: return
        reportFirDiagnostic(errorImport.diagnostic, source, reporter, data)
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

        // Will be handled by [FirDelegatedPropertyChecker]
        if (source.kind == FirFakeSourceElementKind.DelegatedPropertyAccessor &&
            (diagnostic is ConeUnresolvedNameError || diagnostic is ConeAmbiguityError || diagnostic is ConeInapplicableWrongReceiver || diagnostic is ConeInapplicableCandidateError)
        ) {
            return
        }

        if (source.kind == FirFakeSourceElementKind.ImplicitConstructor || source.kind == FirFakeSourceElementKind.DesugaredForLoop) {
            // See FirForLoopChecker
            return
        }
        for (coneDiagnostic in diagnostic.toFirDiagnostics(source, qualifiedAccessSource)) {
            reporter.report(coneDiagnostic, context)
        }
    }
}
