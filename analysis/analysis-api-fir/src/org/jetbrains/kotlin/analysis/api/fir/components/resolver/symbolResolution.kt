/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.resolver

import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.analysis.api.fir.buildSymbol
import org.jetbrains.kotlin.analysis.api.fir.components.KaFirSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.components.analysisSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.components.resolutionFacade
import org.jetbrains.kotlin.analysis.api.fir.getCandidateSymbols
import org.jetbrains.kotlin.analysis.api.fir.getTagIfSubject
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getQualifierSelected
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsByResolvedImport
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsForResolvedQualifier
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.getSymbolsForResolvedTypeRef
import org.jetbrains.kotlin.analysis.api.fir.references.FirReferenceResolveHelper.toTargetSymbol
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirSimpleSymbolResolutionError
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirSimpleSymbolResolutionSuccess
import org.jetbrains.kotlin.analysis.api.fir.unwrapSafeCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleSymbolResolutionError
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.FirScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnreportedDuplicateDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

context(component: KaFirSessionComponent)
internal fun resolveSymbol(psi: KtElement): KaSymbolResolutionAttempt? = when (psi) {
    is KDocName -> resolveKDocName(psi)
    is KtNameReferenceExpression if psi.parent is KtValueArgumentName -> resolveValueArgumentName(psi)
    else -> psi.getOrBuildFirWithAdjustments()?.toKaSymbolResolutionAttempt(psi)
}

context(component: KaFirSessionComponent)
private fun resolveValueArgumentName(psi: KtNameReferenceExpression): KaSymbolResolutionAttempt? {
    val valueArgument = psi.parent.parent as? KtValueArgument ?: return null
    val call = getCallByValueArgument(valueArgument) ?: return null
    call.findCorrespondingParameter(psi.getReferencedNameAsName())?.let {
        return KaFirSimpleSymbolResolutionSuccess(firSymbolBuilder.buildSymbol(it))
    }

    // Without a resolved argument mapping, no candidate is chosen (e.g., the call is ambiguous),
    // so the call error is reported with the parameters that the candidates map the argument to
    val calleeReference = (call as? FirResolvable)?.calleeReference as? FirDiagnosticHolder ?: return null
    val candidateParameters = calleeReference.diagnostic.findCandidateParameters(valueArgument)
    if (candidateParameters.isEmpty()) return null

    return KaFirSimpleSymbolResolutionError(
        backingDiagnostic = calleeReference.createKaDiagnostic(),
        backingCandidateSymbols = candidateParameters.map(firSymbolBuilder::buildSymbol),
    )
}

/**
 * Returns the call that receives [argument], e.g., `foo(x = 1)` for `x = 1`.
 */
context(component: KaFirSessionComponent)
private fun getCallByValueArgument(argument: KtValueArgument): FirCall? {
    val ktValueArgumentList = argument.parent as? KtValueArgumentList ?: return null
    val ktCallExpression = ktValueArgumentList.parent as? KtCallElement ?: return null

    return ktCallExpression.getOrBuildFir(resolutionFacade)?.unwrapSafeCall() as? FirCall
}

/**
 * Returns the parameter that the argument named [name] is mapped to,
 * or `null` if the call has no resolved argument mapping (e.g., the call is ambiguous).
 */
private fun FirCall.findCorrespondingParameter(name: Name): FirValueParameter? {
    return resolvedArgumentMappingIncludingContextArguments?.values?.firstOrNull { it.name == name }
}

/**
 * Returns the distinct parameters that the candidates of [this] diagnostic map [argument] to.
 *
 * The mappings are built by the compiler, so a candidate with several parameters named as the argument
 * contributes only the one the compiler maps the argument to.
 */
private fun ConeDiagnostic.findCandidateParameters(argument: KtValueArgument): List<FirValueParameter> {
    val candidates = when (this) {
        is ConeHiddenCandidateError -> return emptyList()
        is ConeDiagnosticWithCandidates -> candidates
        is ConeUnreportedDuplicateDiagnostic -> return original.findCandidateParameters(argument)
        else -> return emptyList()
    }

    return candidates
        .mapNotNull { (it as? Candidate)?.findParameterMappedTo(argument) }
        .distinct()
}

private fun Candidate.findParameterMappedTo(argument: KtValueArgument): FirValueParameter? {
    if (!argumentMappingInitialized) return null
    return argumentMapping.entries.firstOrNull { it.key.expression.realPsi == argument }?.value
}

context(component: KaFirSessionComponent)
private fun resolveKDocName(psi: KDocName): KaSymbolResolutionAttempt? {
    val fullFqName = generateSequence(psi) { it.parent as? KDocName }.last().getQualifiedNameAsFqName()
    val selectedFqName = psi.getQualifiedNameAsFqName()
    val containedTagSectionIfSubject = psi.getTagIfSubject()?.knownTag

    val symbols = KDocReferenceResolver.resolveKdocFqName(
        analysisSession = analysisSession,
        selectedFqName = selectedFqName,
        fullFqName = fullFqName,
        contextElement = psi,
        containedTagSectionIfSubject = containedTagSectionIfSubject,
    )

    if (symbols.isEmpty()) return null
    return KaFirSimpleSymbolResolutionSuccess(backingSymbols = symbols.toList())
}

context(component: KaFirSessionComponent)
private fun FirElement.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? = when (this) {
    is FirResolvedTypeRef if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
    is FirReference -> toKaSymbolResolutionAttempt(psi)

    // IMPORTANT: all branches above must handle `FirDiagnosticHolder` manually
    is FirDiagnosticHolder -> toKaSymbolResolutionError()
    is FirResolvedTypeRef if psi is KtFunctionType -> toKaSymbolResolutionAttemptForFunctionType()
    is FirResolvable -> toKaSymbolResolutionAttempt(psi)
    is FirReturnExpression -> toKaSymbolResolutionAttempt()
    is FirTypeParameter -> toKaSymbolResolutionAttempt()
    is FirResolvedReifiedParameterReference -> toKaSymbolResolutionAttempt()
    is FirVariableAssignment -> lValue.unwrapExpression().toKaSymbolResolutionAttempt(psi)
    is FirSmartCastExpression -> originalExpression.toKaSymbolResolutionAttempt(psi)
    is FirSafeCallExpression -> unwrapSelector().toKaSymbolResolutionAttempt(psi)
    is FirResolvedQualifier if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
    is FirPackageDirective if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
    is FirResolvedImport if psi is KtSimpleNameExpression -> toKaSymbolResolutionAttempt(psi)
    else -> null
}

context(component: KaFirSessionComponent)
private fun FirResolvable.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? {
    val calleeReference = when {
        /**
         * FIR doesn't have a dedicated element for label in `super@Foo.something()` statement
         *
         * ```kotlin
         * interface A { fun a() {} }
         *
         * class Foo : A {
         *     fun foo() {
         *         super@F<caret>oo.a()
         *     }
         * }
         * ```
         */
        psi is KtLabelReferenceExpression && calleeReference is FirSuperReference && this is FirQualifiedAccessExpression -> {
            (dispatchReceiver as? FirThisReceiverExpression)?.calleeReference
        }

        else -> calleeReference
    }

    return calleeReference?.toKaSymbolResolutionAttempt(psi)
}

context(component: KaFirSessionComponent)
private fun FirReference.toKaSymbolResolutionAttempt(psi: KtElement): KaSymbolResolutionAttempt? {
    val firSymbolToBuild = when (this) {
        is FirSuperReference -> {
            val resolvedTypeRef = superTypeRef as? FirResolvedTypeRef ?: return null
            resolvedTypeRef.toRegularClassSymbol(analysisSession.firSession)
        }

        else -> when (val symbol = symbol) {
            is FirReceiverParameterSymbol if (psi is KtLabelReferenceExpression || symbol.fir is FirScriptReceiverParameter) -> {
                // Label references should refer to the containing declaration symbol (not the receiver parameter symbol)

                // Probably the workaround for a script receiver parameter should be dropped
                // as soon as `KaScriptSymbol` API will be properly designed KT-76360
                // (currently we don't have a dedicated KaSymbol for script receiver parameter)
                symbol.containingDeclarationSymbol
            }

            is FirNamedFunctionSymbol if psi is KtNameReferenceExpression && symbol.name == OperatorNameConventions.INVOKE -> {
                invokeFunctionReceiver(psi)?.let { return it }
                symbol
            }

            else -> symbol
        }
    }

    if (this is FirDiagnosticHolder) {
        return toKaSymbolResolutionError()
    }

    val symbol = when (val symbol = firSymbolToBuild?.buildSymbol(firSymbolBuilder)) {
        is KaConstructorSymbol if (psi is KtNameReferenceExpression || psi is KtEnumEntrySuperclassReferenceExpression) -> with(analysisSession) {
            // Callee reference for a constructor call is supposed to refer to the class
            // while the entire call refers to the constructor.
            // `KaSymbol` instead of `FirSymbol` is checked intentionally to properly support
            // type-aliased constructors
            symbol.containingDeclaration
        }

        else -> symbol
    } ?: return null

    return KaFirSimpleSymbolResolutionSuccess(backingSymbol = symbol)
}

/**
 * [KtNameReferenceExpression] maps directly to the invoke function, so the corresponding [KtCallExpression]
 * has to be checked to get the real callee
 *
 * @see getContainingCallExpressionForCalleeExpression
 */
context(component: KaFirSessionComponent)
private fun invokeFunctionReceiver(psi: KtNameReferenceExpression): KaSymbolResolutionAttempt? {
    val callExpression = psi.getContainingCallExpressionForCalleeExpression() ?: return null
    val implicitInvokeCall = callExpression.getOrBuildFir(analysisSession.resolutionFacade)
        ?.unwrapSafeCall() as? FirImplicitInvokeCall

    return implicitInvokeCall?.explicitReceiver?.toKaSymbolResolutionAttempt(psi)
}

context(component: KaFirSessionComponent)
private fun FirResolvedQualifier.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
    return getSymbolsForResolvedQualifier(
        fir = this,
        expression = psi,
        session = analysisSession.firSession,
        symbolBuilder = firSymbolBuilder,
    ).ifNotEmpty(::KaFirSimpleSymbolResolutionSuccess)
}

context(component: KaFirSessionComponent)
private fun FirPackageDirective.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
    val packageFqName = getQualifierSelected(psi, forQualifiedType = false)
    return firSymbolBuilder.createPackageSymbolIfOneExists(packageFqName)?.let(::KaFirSimpleSymbolResolutionSuccess)
}

context(component: KaFirSessionComponent)
private fun FirTypeParameter.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
    return KaFirSimpleSymbolResolutionSuccess(firSymbolBuilder.buildSymbol(symbol))
}

context(component: KaFirSessionComponent)
private fun FirResolvedReifiedParameterReference.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
    return KaFirSimpleSymbolResolutionSuccess(firSymbolBuilder.buildSymbol(symbol))
}

context(component: KaFirSessionComponent)
private fun FirResolvedImport.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
    return getSymbolsByResolvedImport(
        expression = psi,
        builder = firSymbolBuilder,
        fir = this,
        session = analysisSession.firSession,
    ).ifNotEmpty(::KaFirSimpleSymbolResolutionSuccess)
}

context(component: KaFirSessionComponent)
private fun FirResolvedTypeRef.toKaSymbolResolutionAttemptForFunctionType(): KaSymbolResolutionAttempt? {
    val symbol = toTargetSymbol(analysisSession.firSession, firSymbolBuilder) ?: return null
    return KaFirSimpleSymbolResolutionSuccess(backingSymbol = symbol)
}

context(component: KaFirSessionComponent)
private fun FirResolvedTypeRef.toKaSymbolResolutionAttempt(psi: KtSimpleNameExpression): KaSymbolResolutionAttempt? {
    val resolvedTypeSymbols = getSymbolsForResolvedTypeRef(
        expression = psi,
        fir = this,
        session = analysisSession.firSession,
        symbolBuilder = firSymbolBuilder,
    )

    val resolutionError = (this as? FirDiagnosticHolder)?.toKaSymbolResolutionError()?.let { resolutionError ->
        val name = psi.getReferencedNameAsName()
        KaFirSimpleSymbolResolutionError(
            backingDiagnostic = resolutionError.diagnostic,
            // TODO(KT-85949): replace filtering with a proper error/symbols once the issue is fixed.
            // For now it is used to get rid of unrelated classifiers from the result.
            // No need to check packages since they cannot be candidates
            backingCandidateSymbols = resolutionError.candidateSymbols.filter { it is KaNamedSymbol && it.name == name },
        )
    }

    // Resolved symbols might properly detect usages of nested elements,
    // but at the same time, if they found error symbols, the error result has to be preserved
    val errorCandidates = resolutionError?.candidateSymbols
    if (errorCandidates != null &&
        errorCandidates.size == resolvedTypeSymbols.size &&
        errorCandidates.toHashSet().containsAll(resolvedTypeSymbols)
    ) {
        return resolutionError
    }

    return resolvedTypeSymbols.ifNotEmpty(::KaFirSimpleSymbolResolutionSuccess) ?: resolutionError
}

context(component: KaFirSessionComponent)
private fun FirDiagnosticHolder.toKaSymbolResolutionError(): KaSimpleSymbolResolutionError {
    val candidates = if (this is FirNamedReference) {
        getCandidateSymbols()
    } else {
        diagnostic.getCandidateSymbols()
    }

    return KaFirSimpleSymbolResolutionError(
        backingDiagnostic = createKaDiagnostic(),
        backingCandidateSymbols = candidates.map(firSymbolBuilder::buildSymbol),
    )
}

context(component: KaFirSessionComponent)
private fun FirReturnExpression.toKaSymbolResolutionAttempt(): KaSymbolResolutionAttempt {
    return when (val firFunctionSymbol = target.labeledElement.symbol) {
        is FirErrorFunctionSymbol -> {
            val diagnostic = firFunctionSymbol.fir.createKaDiagnostic()
            KaFirSimpleSymbolResolutionError(backingCandidateSymbols = emptyList(), backingDiagnostic = diagnostic)
        }

        else -> {
            val kaSymbol = firFunctionSymbol.buildSymbol(firSymbolBuilder)
            KaFirSimpleSymbolResolutionSuccess(kaSymbol)
        }
    }
}

/**
 * When resolving the calleeExpression of a `KtCallExpression`, we resolve the entire `KtCallExpression` instead. This way, the
 * corresponding FIR element is the `FirFunctionCall`, etc. Implicit invoke is then specially handled after obtaining the
 * `FirImplicitInvokeCall`.
 *
 * Note that, if the calleeExpression is already a KtCallExpression, then we don't do this because such a callExpression can be properly
 * resolved to the desired FIR element. That is, cases like `getHighLevelFunction()()` just works, where the both `KtCallExpression`
 * resolve to the desired FIR element.
 */
internal fun KtElement.getContainingCallExpressionForCalleeExpression(): KtCallExpression? {
    if (this !is KtExpression) return null
    val calleeExpression = deparenthesize(this) ?: return null
    if (calleeExpression is KtCallExpression) return null
    val callExpression = parentOfType<KtCallExpression>() ?: return null
    if (deparenthesize(callExpression.calleeExpression) != calleeExpression) return null
    return callExpression
}
