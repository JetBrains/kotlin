/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object ActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) return
        val (actual, expect) = matchActualWithNonFinalExpect(declaration, descriptor, context) ?: return

        // The explicit casts won't be necessary when we start compiling kotlin with K2. K1 doesn't build CFG properly
        @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
        declaration as KtClassLikeDeclaration
        @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
        descriptor as ClassifierDescriptorWithTypeParameters

        checkExpectActualScopeDiff(expect, actual, context, declaration, descriptor)
    }
}

private fun checkExpectActualScopeDiff(
    expect: ClassDescriptor,
    actual: ClassDescriptor,
    context: DeclarationCheckerContext,
    declaration: KtClassLikeDeclaration,
    descriptor: ClassifierDescriptorWithTypeParameters,
) {
    val scopeDiff = calculateExpectActualScopeDiff(expect, actual)
    if (scopeDiff.isNotEmpty()) {
        context.trace.report(
            Errors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER_WARNING.on(
                declaration,
                descriptor,
                scopeDiff,
                scopeDiff.first().expectClass, // All expect classes in scopeDiff are the same
            )
        )
    }
    if (descriptor !is TypeAliasDescriptor) {
        for (diff in scopeDiff) {
            // If it can't be reported, it's not a big deal, because this error is already
            // reported on the 'actual class' itself (see code above)
            context.trace.reportIfPossible(diff)
        }
    }
}

@OptIn(ExperimentalContracts::class)
internal fun matchActualWithNonFinalExpect(
    declaration: KtDeclaration,
    descriptor: DeclarationDescriptor,
    context: DeclarationCheckerContext,
): Pair<ClassDescriptor, ClassDescriptor>? {
    contract {
        returnsNotNull() implies (declaration is KtClassLikeDeclaration)
        returnsNotNull() implies (descriptor is ClassifierDescriptorWithTypeParameters)
    }
    if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return null
    if (declaration !is KtTypeAlias && declaration !is KtClassOrObject) return null
    if (descriptor !is TypeAliasDescriptor && descriptor !is ClassDescriptor) return null

    // Common supertype of KtTypeAlias and KtClassOrObject is KtClassLikeDeclaration.
    // Common supertype of TypeAliasDescriptor and ClassDescriptor is ClassifierDescriptorWithTypeParameters.
    // The explicit casts won't be necessary when we start compiling kotlin with K2.
    @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
    declaration as KtClassLikeDeclaration
    @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
    descriptor as ClassifierDescriptorWithTypeParameters

    if (!descriptor.isActual) return null

    val actual = when (descriptor) {
        is ClassDescriptor -> descriptor
        is TypeAliasDescriptor -> descriptor.classDescriptor
        else -> error("ClassifierDescriptorWithTypeParameters has only two inheritors")
    } ?: return null
    // If actual is final then expect is final as well (otherwise another checker will report a diagnostic).
    // There is no need to waste time searching for the appropriate expect and checking its modality. This `if` is an optimization
    if (actual.modality == Modality.FINAL) return null

    val expect = ExpectedActualResolver.findExpectedForActual(descriptor)
        ?.get(K1ExpectActualCompatibility.Compatible)
        ?.singleOrNull() as? ClassDescriptor // if actual has more than one expects then it will be reported by another checker
        ?: return null

    if (expect.modality == Modality.FINAL) return null
    return actual to expect
}

private fun calculateExpectActualScopeDiff(
    expect: ClassDescriptor,
    actual: ClassDescriptor,
): Set<K1ExpectActualMemberDiff<CallableMemberDescriptor, ClassDescriptor>> {
    val matchingContext = ClassicExpectActualMatchingContext(actual.module)
    // It's responsibility of AbstractExpectActualCompatibilityChecker to report that. Most probably this check is redundant,
    // because we can't reach this line if expect and actual don't match, but let's have it for safety
    if (expect.declaredTypeParameters.size != actual.declaredTypeParameters.size) return emptySet()
    val classTypeSubstitutor = matchingContext.createExpectActualTypeParameterSubstitutor(
        expect.declaredTypeParameters,
        actual.declaredTypeParameters,
        parentSubstitutor = null
    )

    val expectClassCallables = expect.unsubstitutedMemberScope.extractNonPrivateCallables()
    val actualClassCallables = actual.unsubstitutedMemberScope.extractNonPrivateCallables()
        .filter { it.kind.isReal } // Filter out fake-overrides from actual because we compare list of supertypes separately anyway

    val nameAndKindToExpectCallables = expectClassCallables.groupBy { it.name to it.functionVsPropertyKind }

    return actualClassCallables.flatMap { actualMember ->
        val potentialExpects = nameAndKindToExpectCallables[actualMember.name to actualMember.functionVsPropertyKind]
        if (potentialExpects.isNullOrEmpty()) {
            emptyList<K1ExpectActualMemberDiff.Kind>()
        } else {
            potentialExpects
                .map { expectMember ->
                    K1AbstractExpectActualCompatibilityChecker.getCallablesCompatibility(
                        expectMember,
                        actualMember,
                        classTypeSubstitutor,
                        expect,
                        actual,
                        matchingContext
                    )
                }
                .takeIf { kinds -> kinds.all { it != K1ExpectActualCompatibility.Compatible } }
                .orEmpty()
                .mapNotNull {
                    when (it) {
                        is K1ExpectActualCompatibility.Compatible -> error("Compatible was filtered out by takeIf")
                        K1ExpectActualCompatibility.Incompatible.CallableKind,
                        K1ExpectActualCompatibility.Incompatible.ParameterCount,
                        K1ExpectActualCompatibility.Incompatible.ParameterShape,
                        K1ExpectActualCompatibility.Incompatible.ParameterTypes,
                        K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterCount,
                        K1ExpectActualCompatibility.Incompatible.FunctionTypeParameterUpperBounds,
                            // Don't report "matching" (aka "strong") incompatibilities, because it's
                            // incompatibilities that happen only when a new member added
                        -> null

                        is K1ExpectActualCompatibility.Incompatible -> it.toMemberDiffKind()
                        // If toMemberDiffKind returns null then some Kotlin invariants described in toMemberDiffKind no longer hold.
                        // We can't throw exception here because it would crash the compilation.
                        // Those broken invariants just needs to be reported by other checkers.
                        // But it's better to report K1ExpectActualMemberDiff.Kind.Unknown to make sure that we don't have missed
                        // compilation errors if the invariants change
                            ?: K1ExpectActualMemberDiff.Kind.Unknown
                    }
                }
        }
            .map { kind -> K1ExpectActualMemberDiff(kind, actualMember, expect) }
    }.toSet()
}

private fun MemberScope.extractNonPrivateCallables(): Sequence<CallableMemberDescriptor> {
    val functions =
        getFunctionNames().asSequence().flatMap { getContributedFunctions(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }
    val properties =
        getVariableNames().asSequence().flatMap { getContributedVariables(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }
    return (functions + properties).filter { !Visibilities.isPrivate(it.visibility.delegate) }
}

private enum class Kind { FUNCTION, PROPERTY }
private val CallableMemberDescriptor.functionVsPropertyKind: Kind
    get() = when (this) {
        is PropertyDescriptor -> Kind.PROPERTY
        is FunctionDescriptor -> Kind.FUNCTION
        else -> error("Unknown kind $this")
    }

private val CallableMemberDescriptor.psiIfReal: KtCallableDeclaration?
    get() = takeIf { it.kind.isReal }?.source?.let { it as? KotlinSourceElement }?.psi as? KtCallableDeclaration

private fun BindingTrace.reportIfPossible(diff: K1ExpectActualMemberDiff<CallableMemberDescriptor, ClassDescriptor>) {
    val psi = diff.actualMember.psiIfReal ?: return
    val diagnostic = when (diff.kind) {
        K1ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride ->
            Errors.RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.ModalityChangedInOverride ->
            Errors.MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.VisibilityChangedInOverride ->
            Errors.VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride ->
            Errors.SETTER_VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on((psi as? KtProperty)?.setter ?: return, diff)
        K1ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride ->
            Errors.PARAMETER_NAME_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride ->
            Errors.PROPERTY_KIND_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.LateinitChangedInOverride ->
            Errors.LATEINIT_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.VarargChangedInOverride ->
            Errors.VARARG_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride ->
            Errors.TYPE_PARAMETER_NAMES_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
        K1ExpectActualMemberDiff.Kind.Unknown ->
            Errors.UNKNOWN_PROBLEM_DURING_NON_FINAL_CLASSIFIER_ACTUALIZATION_WARNING.on(psi, diff)
    }
    report(diagnostic)
}
