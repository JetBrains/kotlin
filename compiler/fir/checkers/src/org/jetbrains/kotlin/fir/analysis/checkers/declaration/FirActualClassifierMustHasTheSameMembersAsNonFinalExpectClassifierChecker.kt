/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.firClassLike
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMemberDiff
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * [K1 counterpart checker][org.jetbrains.kotlin.resolve.checkers.ActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker]
 */
object FirActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val (actual, expect) = matchActualWithNonFinalExpect(declaration, context) ?: return

        // The explicit casts won't be necessary when we start compiling kotlin with K2. K1 doesn't build CFG properly
        declaration as FirClassLikeDeclaration

        checkSupertypes(actual, expect, declaration, context, reporter)
        checkExpectActualScopeDiff(expect, actual, declaration, context, reporter)
    }
}

private fun checkSupertypes(
    actual: FirClassSymbol<*>,
    expect: FirClassSymbol<*>,
    declaration: FirClassLikeDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    val resolvedSuperTypes = actual.resolvedSuperTypes.map { it.fullyExpandedType(context.session) }
    val resolvedSuperTypes1 = expect.resolvedSuperTypes.map { it.fullyExpandedType(context.session) }
    val addedSupertypes = resolvedSuperTypes - resolvedSuperTypes1.toSet()
    if (addedSupertypes.isNotEmpty()) {
        reporter.reportOn(
            declaration.source,
            FirErrors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER,
            declaration.symbol,
            addedSupertypes,
            context
        )
    }
}

private fun checkExpectActualScopeDiff(
    expect: FirClassSymbol<*>,
    actual: FirClassSymbol<*>,
    declaration: FirClassLikeDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    val scopeDiff = calculateExpectActualScopeDiff(expect, actual, context)
    if (scopeDiff.isNotEmpty()) {
        reporter.reportOn(
            declaration.source,
            FirErrors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER,
            declaration.symbol,
            scopeDiff,
            context
        )
    }
    if (declaration !is FirTypeAlias) {
        for (diff in scopeDiff) {
            if (diff.actualMember.getContainingClassSymbol(context.session) == actual) {
                reporter.reportOn(diff.actualMember.source, diff.kind.factory, diff, context)
            }
        }
    }
}

private val allowDifferentMembersInActualFqn = FqName("kotlin.AllowDifferentMembersInActual")

@OptIn(ExperimentalContracts::class)
internal fun matchActualWithNonFinalExpect(
    declaration: FirDeclaration,
    context: CheckerContext,
): Pair<FirClassSymbol<*>, FirClassSymbol<*>>? {
    contract {
        returnsNotNull() implies (declaration is FirClassLikeDeclaration)
    }
    if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return null

    if (declaration !is FirTypeAlias && declaration !is FirClass) return null

    // Common supertype of KtTypeAlias and KtClassOrObject is KtClassLikeDeclaration.
    // Common supertype of TypeAliasDescriptor and ClassDescriptor is ClassifierDescriptorWithTypeParameters.
    // The explicit casts won't be necessary when we start compiling kotlin with K2.
    declaration as FirClassLikeDeclaration

    if (declaration.annotations.any { it.fqName(context.session) == allowDifferentMembersInActualFqn }) return null

    val actual = when (declaration) {
        is FirClass -> declaration.takeIf(FirClass::isActual)?.symbol
        is FirTypeAlias -> declaration.symbol.fullyExpandedClass(context.session)
        else -> error("ClassifierDescriptorWithTypeParameters has only two inheritors")
    } ?: return null
    // If actual is final than expect is final as well (otherwise another checker will report a diagnostic).
    // There is no need to waste time searching for the appropriate expect and checking its modality. This `if` is an optimization
    if (actual.isFinal) return null

    val expect = declaration.symbol.expectForActual
        ?.get(ExpectActualCompatibility.Compatible)
        ?.singleOrNull() as? FirClassSymbol<*> // if actual has more than one expects then it will be reported by another checker
        ?: return null

    if (expect.isFinal) return null
    return actual to expect
}

private fun calculateExpectActualScopeDiff(
    expect: FirClassSymbol<*>,
    actual: FirClassSymbol<*>,
    context: CheckerContext,
): Set<ExpectActualMemberDiff<FirCallableSymbol<*>, FirClassSymbol<*>>> {
    // todo memberRequiredPhase = null
    val expectScope = expect.unsubstitutedScope(context.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
    val actualScope = actual.unsubstitutedScope(context.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
    val expectClassCallables = expectScope.extractNonPrivateCallables(context)
    val nameAndKindToExpectCallable = expectClassCallables.groupBy { it.name to it.kind }
    return (actualScope.extractNonPrivateCallables(context) - expectClassCallables).asSequence()
        .flatMap { unmatchedActualCallable ->
            when (val expectCallablesWithTheSameName =
                nameAndKindToExpectCallable[unmatchedActualCallable.name to unmatchedActualCallable.kind]) {
                null -> listOf(ExpectActualMemberDiff.Kind.NonPrivateCallableAdded)
                else -> expectCallablesWithTheSameName.map {
                    calculateDiffKind(expect = it, actual = unmatchedActualCallable) ?: error("Not equal callables can't have zero diff")
                }
            }.map { kind -> ExpectActualMemberDiff(kind, unmatchedActualCallable.symbol, expect) }
        }
        .toSet()
}

private fun FirContainingNamesAwareScope.extractNonPrivateCallables(context: CheckerContext): Set<Callable> =
    getCallableNames().asSequence<Name>()
        .flatMap { getFunctions(it) + getProperties(it) }
        .filter { !Visibilities.isPrivate(it.visibility) }
        .map { symbol ->
            Callable(
                symbol.name,
                when (symbol) {
                    is FirVariableSymbol -> Kind.PROPERTY
                    is FirFunctionSymbol -> Kind.FUNCTION
                    else -> error("Unknown kind $symbol")
                },
                symbol.receiverParameter?.typeRef?.firClassLike(context.session),
                symbol.resolvedContextReceivers.map { it.typeRef.firClassLike(context.session) ?: error("can't get type") },
                when (symbol) {
                    is FirVariableSymbol -> emptyList()
                    is FirFunctionSymbol -> symbol.valueParameterSymbols.map { Parameter(it.name, it.resolvedReturnType) }
                    else -> error("Unknown kind $symbol")
                },
                symbol.resolvedReturnType,
                symbol.modality ?: error("Can't get modality"),
                symbol.visibility,
                symbol,
            )
        }
        .toSet()

private data class Parameter(val name: Name, val type: ConeKotlinType)
private enum class Kind { FUNCTION, PROPERTY }
private class Callable(
    val name: Name,
    val kind: Kind,
    val extensionReceiverType: FirClassLikeDeclaration?,
    val contextReceiverTypes: List<FirClassLikeDeclaration>,
    val parameters: List<Parameter>,
    val returnType: ConeKotlinType,
    val modality: Modality,
    val visibility: Visibility,
    val symbol: FirCallableSymbol<*>,
) {
    override fun equals(other: Any?): Boolean = other is Callable && calculateDiffKind(this, other) == null
    override fun hashCode(): Int =
        Objects.hash(name, kind, extensionReceiverType, contextReceiverTypes, parameters, returnType, modality, visibility)
}

private fun calculateDiffKind(expect: Callable, actual: Callable): ExpectActualMemberDiff.Kind? = when {
    expect.name != actual.name ||
            expect.kind != actual.kind ||
            expect.extensionReceiverType != actual.extensionReceiverType ||
            expect.contextReceiverTypes != actual.contextReceiverTypes ||
            expect.parameters != actual.parameters -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
    expect.returnType != actual.returnType -> ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride
    expect.modality != actual.modality -> ExpectActualMemberDiff.Kind.ModalityChangedInOverride
    expect.visibility != actual.visibility -> ExpectActualMemberDiff.Kind.VisibilityChangedInOverride
    else -> null
}

private val ExpectActualMemberDiff.Kind.factory: KtDiagnosticFactory1<ExpectActualMemberDiff<FirCallableSymbol<*>, FirClassSymbol<*>>>
    get() = when (this) {
        ExpectActualMemberDiff.Kind.NonPrivateCallableAdded -> FirErrors.NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride -> FirErrors.RETURN_TYPE_COVARIANT_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ModalityChangedInOverride -> FirErrors.MODALITY_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.VisibilityChangedInOverride -> FirErrors.VISIBILITY_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
    }
