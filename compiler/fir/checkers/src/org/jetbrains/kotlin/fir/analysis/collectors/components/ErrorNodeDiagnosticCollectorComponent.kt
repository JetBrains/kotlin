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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeInapplicableCandidateError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.types.*

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
        } as? FirQualifiedAccessExpression
        // Don't report duplicated unresolved reference on annotation entry (already reported on its type)
        if (source.elementType == KtNodeTypes.ANNOTATION_ENTRY && errorNamedReference.diagnostic is ConeUnresolvedNameError) return
        // Already reported in FirConventionFunctionCallChecker
        if (source.kind == FirFakeSourceElementKind.ArrayAccessNameReference &&
            errorNamedReference.diagnostic is ConeUnresolvedNameError
        ) return

        // If the receiver cannot be resolved, we skip reporting any further problems for this call.
        if (qualifiedAccess?.dispatchReceiver.hasUnresolvedNameError() ||
            qualifiedAccess?.extensionReceiver.hasUnresolvedNameError() ||
            qualifiedAccess?.explicitReceiver.hasUnresolvedNameError()
        ) return

        if (reportUninitializedParameter(qualifiedAccess, data)) {
            return
        }

        reportFirDiagnostic(errorNamedReference.diagnostic, source, reporter, data, qualifiedAccess?.source)
    }

    private fun FirExpression?.hasUnresolvedNameError(): Boolean {
        return this?.unresolvedNameError != null
    }

    private val FirExpression.unresolvedNameError: ConeUnresolvedNameError?
        get() {
            val typeRef = if (this is FirAnonymousFunction) this.returnTypeRef else this.typeRef
            return (typeRef as? FirErrorTypeRef)?.diagnostic as? ConeUnresolvedNameError
        }

    private fun reportUninitializedParameter(qualifiedAccess: FirQualifiedAccessExpression?, context: CheckerContext): Boolean {
        if (qualifiedAccess == null) return false
        val unresolvedNameError = qualifiedAccess.unresolvedNameError ?: return false

        val valueParameter = context.containingDeclarations.lastOrNull { valueParameter ->
            valueParameter is FirValueParameter &&
                    valueParameter.defaultValue?.unresolvedNameError == unresolvedNameError
        } as? FirValueParameter ?: return false

        val function = context.containingDeclarations.lastOrNull { function ->
            function is FirFunction<*> &&
                    valueParameter in function.valueParameters &&
                    unresolvedNameError.name in function.valueParameters.map { it.name }
        } as? FirFunction<*> ?: return false

        val valueParameterIndex = function.valueParameters.indexOf(valueParameter)
        val referredParameterIndex = function.valueParameters.map { it.name }.indexOf(unresolvedNameError.name)
        return if (valueParameterIndex <= referredParameterIndex) {
            reporter.reportOn(
                qualifiedAccess.source,
                FirErrors.UNINITIALIZED_PARAMETER,
                function.valueParameters[referredParameterIndex].symbol,
                context
            )
            true
        } else {
            false
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

        // Will be handled by [FirDelegatedPropertyChecker]
        if (source.kind == FirFakeSourceElementKind.DelegatedPropertyAccessor &&
            (diagnostic is ConeUnresolvedNameError || diagnostic is ConeAmbiguityError || diagnostic is ConeInapplicableCandidateError)
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
