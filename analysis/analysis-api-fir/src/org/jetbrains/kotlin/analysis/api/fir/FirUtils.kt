/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.fir.annotations.computeAnnotationArguments
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaAnnotationImpl
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnreportedDuplicateDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Returns `true` if the symbol is for a function named `invoke`.
 */
internal fun FirBasedSymbol<*>.isInvokeFunction() =
    (this as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE

internal fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

internal fun FirFunctionCall.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    calleeReference.getCandidateSymbols()

internal fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
    (this as? FirResolvedNamedReference)?.resolvedSymbol

internal fun FirReference.getResolvedKtSymbolOfNameReference(builder: KaSymbolByFirBuilder): KaSymbol? =
    getResolvedSymbolOfNameReference()?.fir?.let(builder::buildSymbol)

internal fun FirErrorNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    diagnostic.getCandidateSymbols()

internal fun FirNamedReference.getCandidateSymbols(): Collection<FirBasedSymbol<*>> = when (this) {
    is FirResolvedNamedReference -> listOf(resolvedSymbol)
    is FirErrorNamedReference -> getCandidateSymbols()
    else -> emptyList()
}

internal fun ConeDiagnostic.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    when (this) {
        is ConeHiddenCandidateError -> {
            // Candidate with @Deprecated(DeprecationLevel.HIDDEN)
            emptyList()
        }
        is ConeDiagnosticWithCandidates -> candidateSymbols
        is ConeDiagnosticWithSymbol<*> -> listOf(symbol)
        is ConeUnreportedDuplicateDiagnostic -> original.getCandidateSymbols()
        else -> emptyList()
    }

internal fun FirAnnotation.toKaAnnotation(builder: KaSymbolByFirBuilder): KaAnnotation {
    val constructorSymbol = findAnnotationConstructor(this, builder.rootSession)
        ?.let(builder.functionBuilder::buildConstructorSymbol)

    val classId = toAnnotationClassId(builder.rootSession)

    return KaAnnotationImpl(
        classId = classId,
        psi = psi as? KtCallElement,
        useSiteTarget = useSiteTarget,
        lazyArguments = if (this !is FirAnnotationCall || arguments.isNotEmpty())
            lazy { computeAnnotationArguments(this, builder) }
        else
            lazyOf(emptyList()),
        constructorSymbol = constructorSymbol,
        token = builder.token,
    )
}

private fun findAnnotationConstructor(annotation: FirAnnotation, session: LLFirSession): FirConstructorSymbol? {
    if (annotation is FirAnnotationCall) {
        val constructorSymbol = annotation.calleeReference.toResolvedConstructorSymbol()
        if (constructorSymbol != null) {
            return constructorSymbol
        }
    }

    // Handle unresolved annotation calls gracefully
    @OptIn(UnresolvedExpressionTypeAccess::class)
    val annotationClass = annotation.coneTypeOrNull?.toClassSymbol(session)?.fir ?: return null

    // The search is done via scope to force Java enhancement. Annotation class might be a 'FirJavaClass'
    return annotationClass
        .unsubstitutedScope(session, session.getScopeSession(), withForcedTypeCalculator = false, memberRequiredPhase = null)
        .getDeclaredConstructors()
        .singleOrNull()
}

/**
 * Implicit dispatch receiver is present when an extension function declared in object
 * is imported somewhere else and used without directly referencing the object instance
 * itself:
 *
 * ```kt
 * import Foo.bar
 *
 * object Foo { fun String.bar() {} }
 *
 * fun usage() {
 *   "hello".bar() // this call has implicit 'Foo' dispatch receiver
 * }
 * ```
 */
internal val FirResolvedQualifier.isImplicitDispatchReceiver: Boolean
    get() = source?.kind == KtFakeSourceElementKind.ImplicitReceiver

internal fun FirAnnotationContainer.getJvmNameFromAnnotation(session: FirSession, target: AnnotationUseSiteTarget? = null): String? {
    val annotationCalls = getAnnotationsByClassId(JvmStandardClassIds.Annotations.JvmName, session)
    return annotationCalls.firstNotNullOfOrNull { call ->
        call.getStringArgument(StandardNames.NAME, session)
            ?.takeIf { target == null || call.useSiteTarget == target }
    }
}

internal fun FirElement.unwrapSafeCall(): FirElement =
    (this as? FirSafeCallExpression)?.selector ?: this

internal fun FirPropertyAccessorSymbol.isSetterOverride(analysisSession: KaFirSession): Boolean {
    if (isGetter) return false
    if (isOverride) return true

    val propertySymbol = fir.propertySymbol
    if (!propertySymbol.isOverride) return false
    val session = analysisSession.firSession
    val containingClassScope = dispatchReceiverType?.scope(
        session,
        analysisSession.getScopeSessionFor(session),
        CallableCopyTypeCalculator.DoNothing,
        requiredMembersPhase = FirResolvePhase.STATUS,
    ) ?: return false

    val overriddenProperties = containingClassScope.getDirectOverriddenProperties(propertySymbol)
    return overriddenProperties.any { it.isVar }
}