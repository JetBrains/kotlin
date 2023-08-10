/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.fir.annotations.mapAnnotationParameters
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
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

internal fun FirReference.getResolvedKtSymbolOfNameReference(builder: KtSymbolByFirBuilder): KtSymbol? =
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
        is ConeStubDiagnostic -> original.getCandidateSymbols()
        else -> emptyList()
    }

internal fun FirAnnotation.toKtAnnotationApplication(
    useSiteSession: FirSession,
    index: Int,
    arguments: List<KtNamedAnnotationValue> = FirAnnotationValueConverter.toNamedConstantValue(
        mapAnnotationParameters(this),
        useSiteSession,
    )
): KtAnnotationApplicationWithArgumentsInfo = KtAnnotationApplicationWithArgumentsInfo(
    classId = toAnnotationClassId(useSiteSession),
    psi = psi as? KtCallElement,
    useSiteTarget = useSiteTarget,
    arguments = arguments,
    index = index,
)

internal fun FirAnnotation.toKtAnnotationInfo(
    useSiteSession: FirSession,
    index: Int,
): KtAnnotationApplicationInfo = KtAnnotationApplicationInfo(
    classId = toAnnotationClassId(useSiteSession),
    psi = psi as? KtCallElement,
    useSiteTarget = useSiteTarget,
    isCallWithArguments = this is FirAnnotationCall && arguments.isNotEmpty(),
    index = index,
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
        call.getStringArgument(StandardNames.NAME)
            ?.takeIf { target == null || call.useSiteTarget == target }
    }
}
