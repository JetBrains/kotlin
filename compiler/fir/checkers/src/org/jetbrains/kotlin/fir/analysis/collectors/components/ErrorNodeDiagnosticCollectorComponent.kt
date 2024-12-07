/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.delegatedPropertySourceOrThis
import org.jetbrains.kotlin.fir.analysis.checkers.getReturnedExpressions
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.ConeErrorType

class ErrorNodeDiagnosticCollectorComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    override fun visitErrorLoop(errorLoop: FirErrorLoop, data: CheckerContext) {
        val source = errorLoop.source
        reportFirDiagnostic(errorLoop.diagnostic, source, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        if (errorTypeRef.isLambdaReturnTypeRefThatDoesntNeedReporting(data)) return
        if (errorTypeRef.hasExpandedTypeAliasDeclarationSiteError()) return

        reportFirDiagnostic(errorTypeRef.diagnostic, errorTypeRef.source, data)
    }

    /**
     * Returns true if this [FirErrorTypeRef] is the implicit return type ref of a lambda and the diagnostic doesn't need to be reported.
     * More specifically, the diagnostic can be skipped if it's duplicated in the outer call or in a return expression of the lambda.
     */
    private fun FirErrorTypeRef.isLambdaReturnTypeRefThatDoesntNeedReporting(data: CheckerContext): Boolean {
        if (source?.kind != KtFakeSourceElementKind.ImplicitFunctionReturnType) return false

        val containingDeclaration = data.containingDeclarations.lastOrNull()
        if (containingDeclaration !is FirAnonymousFunction || containingDeclaration.returnTypeRef != this) return false

        return containingDeclaration.getReturnedExpressions().any { it.hasDiagnostic(diagnostic) } ||
                data.callsOrAssignments.any { it is FirExpression && it.hasDiagnostic(diagnostic) }
    }

    /**
     * Returns true if this [FirErrorTypeRef] contains an expanded typealias type with an error,
     * i.e., the error originates from the typealias itself.
     * In this case, we don't need to report anything because the error will already be reported on the declaration site.
     */
    private fun FirErrorTypeRef.hasExpandedTypeAliasDeclarationSiteError(): Boolean {
        val lowerBound = coneType.lowerBoundIfFlexible() as? ConeErrorType ?: return false
        if (lowerBound.diagnostic != this.diagnostic) return false
        return lowerBound.abbreviatedType != null
    }

    private fun FirExpression.hasDiagnostic(diagnostic: ConeDiagnostic): Boolean {
        if ((resolvedType as? ConeErrorType)?.diagnostic == diagnostic) return true
        if ((toReference(session) as? FirDiagnosticHolder)?.diagnostic == diagnostic) return true
        return false
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: CheckerContext) {
        assert(resolvedTypeRef.coneType !is ConeErrorType) {
            "Instead use FirErrorTypeRef for ${resolvedTypeRef.coneType.renderForDebugging()}"
        }
    }

    override fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: CheckerContext) {
        val source = errorAnnotationCall.source
        reportFirDiagnostic(errorAnnotationCall.diagnostic, source, data)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: CheckerContext) {
        processErrorReference(errorNamedReference, errorNamedReference.diagnostic, data)
    }

    override fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference, data: CheckerContext) {
        processErrorReference(resolvedErrorReference, resolvedErrorReference.diagnostic, data)
    }

    private fun processErrorReference(reference: FirNamedReference, diagnostic: ConeDiagnostic, context: CheckerContext) {
        var source = reference.source
        val callOrAssignment = context.callsOrAssignments.lastOrNull()?.takeIf {
            // Use the source of the enclosing FirQualifiedAccess if it is exactly the call to the erroneous callee.
            it.toReference(session) == reference
        }
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source?.elementType == KtNodeTypes.ANNOTATION_ENTRY && diagnostic is ConeUnresolvedNameError) return
        // Already reported in FirConventionFunctionCallChecker
        if (source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference &&
            diagnostic is ConeUnresolvedNameError
        ) return

        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        if (callOrAssignment is FirQualifiedAccessExpression) {
            if (callOrAssignment.dispatchReceiver.cannotBeResolved() ||
                callOrAssignment.extensionReceiver.cannotBeResolved() ||
                callOrAssignment.explicitReceiver.cannotBeResolved()
            ) return
        }

        source = source?.delegatedPropertySourceOrThis(context)

        reportFirDiagnostic(diagnostic, source, context, callOrAssignment?.source)
    }

    private fun FirExpression?.cannotBeResolved(): Boolean {
        return when (val diagnostic = (this?.resolvedType?.lowerBoundIfFlexible() as? ConeErrorType)?.diagnostic) {
            is ConeUnresolvedNameError, is ConeInstanceAccessBeforeSuperCall, is ConeAmbiguousSuper -> true
            is ConeSimpleDiagnostic -> diagnostic.kind == DiagnosticKind.NotASupertype ||
                    diagnostic.kind == DiagnosticKind.SuperNotAvailable ||
                    diagnostic.kind == DiagnosticKind.UnresolvedLabel ||
                    diagnostic.kind == DiagnosticKind.AmbiguousLabel
            else -> false
        }
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression, data: CheckerContext) {
        val source = errorExpression.source
        val diagnostic = errorExpression.diagnostic
        if (source == null) {
            // ConeSyntaxDiagnostic and DiagnosticKind.ExpressionExpected with no source (see check above) are typically symptoms of some
            // syntax error that was already reported during parsing.
            if (diagnostic is ConeSyntaxDiagnostic) return
            if (diagnostic is ConeSimpleDiagnostic && diagnostic.kind == DiagnosticKind.ExpressionExpected) return
        }
        if (diagnostic == ConeContextParameterWithDefaultValue &&
            data.containingDeclarations.let { it.elementAtOrNull(it.lastIndex - 1) } is FirPrimaryConstructor
        ) return
        reportFirDiagnostic(diagnostic, source, data)
    }

    override fun visitErrorFunction(errorFunction: FirErrorFunction, data: CheckerContext) {
        val source = errorFunction.source
        reportFirDiagnostic(errorFunction.diagnostic, source, data)
    }

    override fun visitErrorProperty(errorProperty: FirErrorProperty, data: CheckerContext) {
        val source = errorProperty.source
        reportFirDiagnostic(errorProperty.diagnostic, source, data)
    }

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: CheckerContext) {
        // Only report an error on the outermost parent.
        if (errorResolvedQualifier.explicitParent?.hasErrorOrParentWithError() == true) return

        val source = errorResolvedQualifier.source
        reportFirDiagnostic(errorResolvedQualifier.diagnostic, source, data)
    }

    private fun FirResolvedQualifier.hasErrorOrParentWithError(): Boolean {
        if (this is FirErrorResolvedQualifier) return true
        return explicitParent?.hasErrorOrParentWithError() == true
    }

    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: CheckerContext) {
        reportFirDiagnostic(errorPrimaryConstructor.diagnostic, errorPrimaryConstructor.source, data)
    }

    override fun visitThisReference(thisReference: FirThisReference, data: CheckerContext) {
        val diagnostic = thisReference.diagnostic ?: return
        // FirImplicitThisReference has no source, in this case use source of containing ThisReceiverExpression
        val source = thisReference.source ?: data.containingElements.elementAtOrNull(1)?.source
        reportFirDiagnostic(diagnostic, source, data)
    }

    private fun reportFirDiagnostic(
        diagnostic: ConeDiagnostic,
        source: KtSourceElement?,
        context: CheckerContext,
        callOrAssignmentSource: KtSourceElement? = null,
    ) {
        reportFirDiagnostic(diagnostic, source, context, session, reporter, callOrAssignmentSource)
    }

    companion object {
        internal fun reportFirDiagnostic(
            diagnostic: ConeDiagnostic,
            source: KtSourceElement?,
            context: CheckerContext,
            session: FirSession = context.session,
            reporter: DiagnosticReporter,
            callOrAssignmentSource: KtSourceElement? = null,
        ) {
            // Will be handled by [FirDestructuringDeclarationChecker]
            if (source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY) {
                return
            }

            // Will be handled by [FirDelegatedPropertyChecker]
            if (source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor &&
                (diagnostic is ConeUnresolvedNameError || diagnostic is ConeAmbiguityError || diagnostic is ConeInapplicableWrongReceiver || diagnostic is ConeInapplicableCandidateError)
            ) {
                return
            }

            if (source?.kind == KtFakeSourceElementKind.ImplicitConstructor || source?.kind == KtFakeSourceElementKind.DesugaredForLoop) {
                // See FirForLoopChecker
                return
            }

            // Prefix inc/dec on array access will have two calls to .get(...), don't report for the second one.
            if (source?.kind is KtFakeSourceElementKind.DesugaredPrefixSecondGetReference) {
                return
            }

            for (coneDiagnostic in diagnostic.toFirDiagnostics(session, source, callOrAssignmentSource)) {
                reporter.report(coneDiagnostic, context)
            }
        }
    }
}
