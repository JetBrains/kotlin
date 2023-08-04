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
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
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
    val addedSupertypes = actual.resolvedSuperTypes.mapNotNull { it.fullyExpandedType(context.session).classId?.asSingleFqName() } -
            expect.resolvedSuperTypes.mapNotNull { it.fullyExpandedType(context.session).classId?.asSingleFqName() }.toSet()
    if (addedSupertypes.isNotEmpty()) {
        reporter.reportOn(
            declaration.source,
            FirErrors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER,
            declaration.symbol,
            addedSupertypes.map(FqName::shortName),
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
    val expectScope =
        expect.unsubstitutedScope(context.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
    val actualScope =
        actual.unsubstitutedScope(context.session, context.scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)

    val classTypeSubstitutor =
        createExpectActualTypeParameterSubstitutor(expect.typeParameterSymbols, actual.typeParameterSymbols, context.session)
    val expectClassCallables = expectScope.extractNonPrivateCallables(classTypeSubstitutor, ExpectActual.EXPECT)
    val nameAndKindToExpectCallable = expectClassCallables.groupBy { it.name to it.kind }
    return (actualScope.extractNonPrivateCallables(classTypeSubstitutor, ExpectActual.ACTUAL) - expectClassCallables).asSequence()
        .flatMap { unmatchedActualCallable: Callable ->
            when (val expectCallablesWithTheSameName =
                nameAndKindToExpectCallable[unmatchedActualCallable.name to unmatchedActualCallable.kind]) {
                null -> listOf(ExpectActualMemberDiff.Kind.NonPrivateCallableAdded)
                else -> expectCallablesWithTheSameName.map {
                    calculateExpectActualMemberDiffKind(expect = it, actual = unmatchedActualCallable)
                        ?: error("Not equal callables can't have zero diff")
                }
            }.map { kind -> ExpectActualMemberDiff(kind, unmatchedActualCallable.symbol, expect) }
        }
        .toSet()
}

private fun FirContainingNamesAwareScope.extractNonPrivateCallables(
    classTypeSubstitutor: ConeSubstitutor,
    expectActual: ExpectActual,
): Set<Callable> =
    getCallableNames().asSequence()
        .flatMap { getFunctions(it) + getProperties(it) }
        .filter { !Visibilities.isPrivate(it.visibility) }
        .map { Callable(it, expectActual, classTypeSubstitutor) }
        .toSet()

private data class Parameter(val name: Name, val type: ConeKotlinType)
private enum class Kind { FUNCTION, PROPERTY }
private enum class ExpectActual { EXPECT, ACTUAL }
private class Callable(
    val symbol: FirCallableSymbol<*>,
    val expectActual: ExpectActual,
    val classTypeSubstitutor: ConeSubstitutor,
) {
    val name: Name = symbol.name
    val kind: Kind = when (symbol) {
        is FirVariableSymbol -> Kind.PROPERTY
        is FirFunctionSymbol -> Kind.FUNCTION
        else -> error("Unknown kind $symbol")
    }
    val extensionReceiverType: ConeKotlinType? = symbol.receiverParameter?.typeRef?.coneType
    val contextReceiverTypes: List<ConeKotlinType> = symbol.resolvedContextReceivers.map { it.typeRef.coneType }
    val parameters: List<Parameter> = when (symbol) {
        is FirVariableSymbol -> emptyList()
        is FirFunctionSymbol -> symbol.valueParameterSymbols.map { Parameter(it.name, it.resolvedReturnType) }
        else -> error("Unknown kind $symbol")
    }
    val returnType: ConeKotlinType = symbol.resolvedReturnType
    val modality: Modality = symbol.modality ?: error("Can't get modality")
    val visibility: Visibility = symbol.visibility

    override fun equals(other: Any?): Boolean {
        if (other !is Callable) return false
        check(classTypeSubstitutor === other.classTypeSubstitutor)
        return if (expectActual == other.expectActual) {
            name == other.name &&
                    kind == other.kind &&
                    extensionReceiverType == other.extensionReceiverType &&
                    contextReceiverTypes == other.contextReceiverTypes &&
                    parameters == other.parameters &&
                    returnType == other.returnType &&
                    modality == other.modality &&
                    visibility == other.visibility
        } else {
            val (expect, actual) = if (expectActual == ExpectActual.EXPECT) this to other else other to this
            calculateExpectActualMemberDiffKind(expect, actual) == null
        }
    }

    override fun hashCode(): Int = // Don't hash types because type comparison is complicated
        Objects.hash(
            name,
            kind,
            extensionReceiverType != null,
            contextReceiverTypes.size,
            parameters.map(Parameter::name),
            modality,
            visibility
        )
}

private fun areCompatibleWithSubstitution(
    expect: ConeKotlinType?,
    actual: ConeKotlinType?,
    actualSession: FirSession,
    substitutor: ConeSubstitutor,
): Boolean = areCompatibleExpectActualTypes(expect?.let(substitutor::substituteOrSelf), actual, actualSession)

private fun areCompatibleListWithSubstitution(
    expect: List<ConeKotlinType>,
    actual: List<ConeKotlinType>,
    actualSession: FirSession,
    substitutor: ConeSubstitutor,
): Boolean = expect.size == actual.size && expect.asSequence().zip(actual.asSequence())
    .all { (a, b) -> areCompatibleWithSubstitution(a, b, actualSession, substitutor) }

private fun calculateExpectActualMemberDiffKind(expect: Callable, actual: Callable): ExpectActualMemberDiff.Kind? {
    check(expect.expectActual == ExpectActual.EXPECT)
    check(actual.expectActual == ExpectActual.ACTUAL)
    check(expect.classTypeSubstitutor === actual.classTypeSubstitutor)
    val actualSession = actual.symbol.moduleData.session
    val substitutor = createExpectActualTypeParameterSubstitutor(
        expect.symbol.typeParameterSymbols,
        actual.symbol.typeParameterSymbols,
        actualSession,
        actual.classTypeSubstitutor
    )
    return when {
        expect.name != actual.name ||
                expect.kind != actual.kind ||
                !areCompatibleWithSubstitution(expect.extensionReceiverType, actual.extensionReceiverType, actualSession, substitutor) ||
                !areCompatibleListWithSubstitution(expect.contextReceiverTypes, actual.contextReceiverTypes, actualSession, substitutor) ||
                !areCompatibleListWithSubstitution(
                    expect.parameters.map(Parameter::type),
                    actual.parameters.map(Parameter::type),
                    actualSession,
                    substitutor
                ) -> ExpectActualMemberDiff.Kind.NonPrivateCallableAdded

        expect.parameters.map(Parameter::name) != actual.parameters.map(Parameter::name) ->
            ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride

        !areCompatibleWithSubstitution(expect.returnType, actual.returnType, actualSession, substitutor) ->
            ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride

        expect.modality != actual.modality -> ExpectActualMemberDiff.Kind.ModalityChangedInOverride

        expect.visibility != actual.visibility -> ExpectActualMemberDiff.Kind.VisibilityChangedInOverride

        else -> null
    }
}

private val ExpectActualMemberDiff.Kind.factory: KtDiagnosticFactory1<ExpectActualMemberDiff<FirCallableSymbol<*>, FirClassSymbol<*>>>
    get() = when (this) {
        ExpectActualMemberDiff.Kind.NonPrivateCallableAdded -> FirErrors.NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride -> FirErrors.RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ModalityChangedInOverride -> FirErrors.MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.VisibilityChangedInOverride -> FirErrors.VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride -> FirErrors.PARAMETER_NAME_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride -> TODO()
    }
