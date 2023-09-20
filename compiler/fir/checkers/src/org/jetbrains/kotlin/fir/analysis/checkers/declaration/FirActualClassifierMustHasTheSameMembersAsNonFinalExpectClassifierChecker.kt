/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.createExpectActualTypeParameterSubstitutor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMemberDiff
import org.jetbrains.kotlin.resolve.multiplatform.toMemberDiffKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * [K1 counterpart checker][org.jetbrains.kotlin.resolve.checkers.ActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker]
 */
object FirActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        val (actual, expect) = matchActualWithNonFinalExpect(declaration, context) ?: return

        // The explicit casts won't be necessary when we start compiling kotlin with K2. K1 doesn't build CFG properly
        check(declaration is FirClassLikeDeclaration)

        checkSupertypes(actual, expect, declaration, context, reporter)
        checkExpectActualScopeDiff(expect, actual, declaration, context, reporter)
    }
}

private fun checkSupertypes(
    actual: FirRegularClassSymbol,
    expect: FirRegularClassSymbol,
    declaration: FirClassLikeDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
) {
    val addedSupertypes =
        actual.resolvedSuperTypes.mapNotNull { it.fullyExpandedType(actual.moduleData.session).classId?.asSingleFqName() } -
                // We use actual FirSession to analyze expect declaration, and it's not mistake. We do so because otherwise different
                // supertypes modulo `typealias`/`actual typealias` are reported.
                expect.resolvedSuperTypes.mapNotNull { it.fullyExpandedType(actual.moduleData.session).classId?.asSingleFqName() }.toSet()
    if (addedSupertypes.isNotEmpty()) {
        reporter.reportOn(
            declaration.source,
            FirErrors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER,
            declaration.symbol,
            addedSupertypes.map(FqName::shortName),
            expect,
            context
        )
    }
}

private fun checkExpectActualScopeDiff(
    expect: FirRegularClassSymbol,
    actual: FirRegularClassSymbol,
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
            expect,
            context
        )
    }
    if (declaration !is FirTypeAlias) {
        for (diff in scopeDiff) {
            if (diff.actualMember.getContainingClassSymbol(context.session) == actual) {
                // If it can't be reported, it's not a big deal, because this error is already
                // reported on the 'actual class' itself (see code above)
                reporter.reportIfPossible(diff, context)
            }
        }
    }
}

private val allowDifferentMembersInActualFqn = FqName("kotlin.AllowDifferentMembersInActual")

@OptIn(ExperimentalContracts::class)
internal fun matchActualWithNonFinalExpect(
    declaration: FirDeclaration,
    context: CheckerContext,
): Pair<FirRegularClassSymbol, FirRegularClassSymbol>? {
    contract {
        returnsNotNull() implies (declaration is FirClassLikeDeclaration)
    }
    if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return null
    if (declaration !is FirTypeAlias && declaration !is FirClass) return null

    // Common supertype of KtTypeAlias and KtClassOrObject is KtClassLikeDeclaration.
    // Common supertype of TypeAliasDescriptor and ClassDescriptor is ClassifierDescriptorWithTypeParameters.
    // The explicit casts won't be necessary when we start compiling kotlin with K2.
    declaration as FirClassLikeDeclaration

    if (!declaration.isActual) return null

    if (declaration.annotations.any { it.fqName(context.session) == allowDifferentMembersInActualFqn }) return null

    val actual = when (declaration) {
        is FirClass -> declaration.symbol as? FirRegularClassSymbol
        is FirTypeAlias -> declaration.symbol.fullyExpandedClass(context.session)
        else -> error("ClassifierDescriptorWithTypeParameters has only two inheritors")
    } ?: return null
    // If actual is final then expect is final as well (otherwise another checker will report a diagnostic).
    // There is no need to waste time searching for the appropriate expect and checking its modality. This `if` is an optimization
    if (actual.isFinal) return null

    val expect = declaration.symbol.expectForActual
        ?.get(ExpectActualCompatibility.Compatible)
        ?.singleOrNull() as? FirRegularClassSymbol // if actual has more than one expects then it will be reported by another checker
        ?: return null

    if (expect.isFinal) return null
    return actual to expect
}

private fun calculateExpectActualScopeDiff(
    expect: FirRegularClassSymbol,
    actual: FirRegularClassSymbol,
    context: CheckerContext,
): Set<ExpectActualMemberDiff<FirCallableSymbol<*>, FirRegularClassSymbol>> {
    if (expect.typeParameterSymbols.size != actual.typeParameterSymbols.size) return emptySet()
    val classTypeSubstitutor = createExpectActualTypeParameterSubstitutor(
        // It's responsibility of AbstractExpectActualCompatibilityChecker to report that
        expect.typeParameterSymbols,
        actual.typeParameterSymbols,
        context.session
    )

    val matchingContext = context.session.expectActualMatchingContextFactory.create(
        context.session,
        context.scopeSession,
        shouldCheckReturnTypesOfCallables = true
    )

    val expectClassCallables = expect
        .unsubstitutedScope(expect.moduleData.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
        .extractNonPrivateCallables()
    val actualClassCallables = actual
        .unsubstitutedScope(actual.moduleData.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
        .extractNonPrivateCallables()
        // Filter out fake-overrides from actual because we compare list of supertypes separately anyway
        .filter { it.getContainingClassSymbol(actual.moduleData.session) == actual }

    val nameAndKindToExpectCallable = expectClassCallables.groupBy { it.name to it.functionVsPropertyKind }

    return actualClassCallables.flatMap { actualMember ->
        val potentialExpects = nameAndKindToExpectCallable[actualMember.name to actualMember.functionVsPropertyKind]
        if (potentialExpects.isNullOrEmpty()) {
            listOf(ExpectActualMemberDiff.Kind.NonPrivateCallableAdded)
        } else {
            potentialExpects
                .map { expectMember ->
                    AbstractExpectActualCompatibilityChecker.getCallablesCompatibility(
                        expectMember,
                        actualMember,
                        classTypeSubstitutor,
                        expect,
                        actual,
                        matchingContext
                    )
                }
                .takeIf { kinds -> kinds.all { it != ExpectActualCompatibility.Compatible } }
                .orEmpty()
                .map {
                    when (it) {
                        ExpectActualCompatibility.Compatible -> error("Compatible was filterd out by takeIf")
                        is ExpectActualCompatibility.Incompatible -> it.toMemberDiffKind()
                        // If toMemberDiffKind returns null then some Kotlin invariants described in toMemberDiffKind no longer hold.
                        // We can't throw exception here because it would crash the compilation.
                        // Those broken invariants just needs to be reported by other checkers.
                        // But it's better to report some error (ExpectActualMemberDiff.Kind.NonPrivateCallableAdded in our case) to
                        // make sure that we don't have missed compilation errors if the invariants change
                            ?: ExpectActualMemberDiff.Kind.NonPrivateCallableAdded
                    }
                }
        }
            .map { kind -> ExpectActualMemberDiff(kind, actualMember, expect) }
    }.toSet()
}

private fun FirContainingNamesAwareScope.extractNonPrivateCallables(): Sequence<FirCallableSymbol<*>> =
    getCallableNames().asSequence()
        .flatMap { getFunctions(it) + getProperties(it) }
        .filter { !Visibilities.isPrivate(it.visibility) }

private enum class Kind { FUNCTION, PROPERTY }
private val FirCallableSymbol<*>.functionVsPropertyKind: Kind
    get() = when (this) {
        is FirVariableSymbol -> Kind.PROPERTY
        is FirFunctionSymbol -> Kind.FUNCTION
        else -> error("Unknown kind $this")
    }

private fun DiagnosticReporter.reportIfPossible(
    diff: ExpectActualMemberDiff<FirCallableSymbol<*>, FirRegularClassSymbol>,
    context: CheckerContext
) {
    val callable = diff.actualMember
    val (factory, reportOn) = when (diff.kind) {
        ExpectActualMemberDiff.Kind.NonPrivateCallableAdded ->
            FirErrors.NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride ->
            FirErrors.RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.ModalityChangedInOverride ->
            FirErrors.MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.VisibilityChangedInOverride ->
            FirErrors.VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride ->
            FirErrors.PARAMETER_NAME_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride ->
            FirErrors.PROPERTY_KIND_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.LateinitChangedInOverride ->
            FirErrors.LATEINIT_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
        ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride ->
            FirErrors.SETTER_VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to
                    ((callable as? FirPropertySymbol)?.setterSymbol ?: return)
        ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride ->
            FirErrors.TYPE_PARAMETER_NAMES_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION to callable
    }
    reportOn(reportOn.source, factory, diff, context)
}
