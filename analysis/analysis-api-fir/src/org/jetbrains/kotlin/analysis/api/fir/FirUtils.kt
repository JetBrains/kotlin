/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.fir.annotations.mapAnnotationParameters
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnreportedDuplicateDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithCandidates
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeDiagnosticWithSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeHiddenCandidateError
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Returns `true` if the symbol is for a function named `invoke`.
 */
internal fun FirBasedSymbol<*>.isInvokeFunction() =
    (this as? FirNamedFunctionSymbol)?.fir?.name == OperatorNameConventions.INVOKE

fun FirFunctionCall.getCalleeSymbol(): FirBasedSymbol<*>? =
    calleeReference.getResolvedSymbolOfNameReference()

fun FirFunctionCall.getCandidateSymbols(): Collection<FirBasedSymbol<*>> =
    calleeReference.getCandidateSymbols()

fun FirReference.getResolvedSymbolOfNameReference(): FirBasedSymbol<*>? =
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

internal fun FirAnnotation.toKtAnnotationApplication(
    builder: KaSymbolByFirBuilder,
    index: Int,
    arguments: List<KaNamedAnnotationValue> = FirAnnotationValueConverter.toNamedConstantValue(
        builder.analysisSession,
        mapAnnotationParameters(this),
        builder,
    )
): KaAnnotationApplicationWithArgumentsInfo {
    val constructorSymbol = (this as? FirAnnotationCall)?.calleeReference
        ?.toResolvedConstructorSymbol()
        ?.let(builder.functionLikeBuilder::buildConstructorSymbol)

    return KaAnnotationApplicationWithArgumentsInfo(
        classId = toAnnotationClassId(builder.rootSession),
        psi = psi as? KtCallElement,
        useSiteTarget = useSiteTarget,
        arguments = arguments,
        index = index,
        constructorSymbolPointer = with(builder.analysisSession) { constructorSymbol?.createPointer() },
        token = builder.token,
    )
}

internal fun FirAnnotation.toKtAnnotationInfo(
    useSiteSession: FirSession,
    index: Int,
    token: KaLifetimeToken
): KaAnnotationApplicationInfo = KaAnnotationApplicationInfo(
    classId = toAnnotationClassId(useSiteSession),
    psi = psi as? KtCallElement,
    useSiteTarget = useSiteTarget,
    isCallWithArguments = this is FirAnnotationCall && arguments.isNotEmpty(),
    index = index,
    token = token
)

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

fun FirAnnotationContainer.getJvmNameFromAnnotation(session: FirSession, target: AnnotationUseSiteTarget? = null): String? {
    val annotationCalls = getAnnotationsByClassId(JvmStandardClassIds.Annotations.JvmName, session)
    return annotationCalls.firstNotNullOfOrNull { call ->
        call.getStringArgument(StandardNames.NAME, session)
            ?.takeIf { target == null || call.useSiteTarget == target }
    }
}

internal fun FirElement.unwrapSafeCall(): FirElement =
    (this as? FirSafeCallExpression)?.selector ?: this
