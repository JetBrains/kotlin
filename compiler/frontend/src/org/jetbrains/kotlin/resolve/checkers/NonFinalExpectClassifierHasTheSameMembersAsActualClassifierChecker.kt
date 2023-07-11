/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * [K2 counterpart checker][org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNonFinalExpectClassifierHasTheSameMembersAsActualClassifierChecker]
 */
object NonFinalExpectClassifierHasTheSameMembersAsActualClassifierChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val (actual, expect) = matchActualWithNonFinalExpect(declaration, descriptor, context) ?: return

        // The explicit casts won't be necessary when we start compiling kotlin with K2. K1 doesn't build CFG properly
        declaration as KtClassLikeDeclaration
        descriptor as ClassifierDescriptorWithTypeParameters

        checkSupertypes(actual, expect, context, declaration, descriptor)
        checkExpectActualScopeDiff(expect, actual, context, declaration, descriptor)
    }
}

data class ExpectActualMemberDiff(val kind: Kind, val actualMember: CallableMemberDescriptor, val expectClass: ClassDescriptor) {
    /**
     * Diff kinds that are legal for fake-overrides in final `expect class`, but illegal for non-final `expect class`
     */
    enum class Kind(val rawMessage: String) {
        NonPrivateCallableAdded(
            "{0}: Non-private member must be declared in the expect class as well. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        ReturnTypeCovariantOverride(
            "{0}: The return type of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        ModalityChangedInOverride(
            "{0}: The modality of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),

        VisibilityChangedInOverride(
            "{0}: The visibility of this member must be the same in the expect class and the actual class. " +
                    "This error happens because the expect class ''{1}'' is non-final."
        ),
    }
}

private fun checkSupertypes(
    actual: ClassDescriptor,
    expect: ClassDescriptor,
    context: DeclarationCheckerContext,
    declaration: KtClassLikeDeclaration,
    descriptor: ClassifierDescriptorWithTypeParameters,
) {
    val addedSupertypes = (actual.getSuperInterfaces() + listOfNotNull(actual.getSuperClassNotAny())).map(ClassDescriptor::fqNameSafe) -
            (expect.getSuperInterfaces() + listOfNotNull(expect.getSuperClassNotAny())).map(ClassDescriptor::fqNameSafe).toSet()

    if (addedSupertypes.isNotEmpty()) {
        context.trace.report(
            Errors.NON_FINAL_EXPECT_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_ACTUAL_CLASSIFIER
                .on(declaration, descriptor, addedSupertypes.map(FqName::shortName))
        )
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
            Errors.NON_FINAL_EXPECT_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_ACTUAL_CLASSIFIER.on(declaration, descriptor, scopeDiff)
        )
    }
    if (descriptor !is TypeAliasDescriptor) {
        for (diff in scopeDiff) {
            // If there is no source to report the error, it's not a big deal, because this error is already
            // reported on the 'actual class' itself (see code above)
            context.trace.report(diff.kind.factory.on(diff.actualMember.psiIfReal ?: continue, diff))
        }
    }
}

private val allowDifferentMembersInActualFqn = FqName("kotlin.AllowDifferentMembersInActual")

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
    declaration as KtClassLikeDeclaration
    descriptor as ClassifierDescriptorWithTypeParameters

    with(OptInUsageChecker) {
        if (declaration.isDeclarationAnnotatedWith(allowDifferentMembersInActualFqn, context.trace.bindingContext)) return null
    }

    val actual = when (descriptor) {
        is ClassDescriptor -> descriptor.takeIf(MemberDescriptor::isActual)
        is TypeAliasDescriptor -> descriptor.classDescriptor
        else -> error("ClassifierDescriptorWithTypeParameters has only two inheritors")
    } ?: return null
    // If actual is final than expect is final as well (otherwise another checker will report a diagnostic).
    // There is no need to waste time searching for the appropriate expect and checking its modality. This `if` is an optimization
    if (actual.modality == Modality.FINAL) return null

    val expect = ExpectedActualResolver.findExpectedForActual(descriptor)
        ?.get(ExpectActualCompatibility.Compatible)
        ?.singleOrNull() as? ClassDescriptor // if actual has more than one expects then it will be reported by another checker
        ?: return null

    if (expect.modality == Modality.FINAL) return null
    return actual to expect
}

private fun calculateExpectActualScopeDiff(
    expect: ClassDescriptor,
    actual: ClassDescriptor,
): Set<ExpectActualMemberDiff> {
    val expectScope = expect.unsubstitutedMemberScope
    val actualScope = actual.unsubstitutedMemberScope
    val expectClassCallables = expectScope.extractNonPrivateCallables()
    val nameAndKindToExpectCallable = expectClassCallables.groupBy { it.name to it.kind }
    return (actualScope.extractNonPrivateCallables() - expectClassCallables).asSequence()
        .flatMap { unmatchedActualCallable ->
            when (val expectCallablesWithTheSameName =
                nameAndKindToExpectCallable[unmatchedActualCallable.name to unmatchedActualCallable.kind]) {
                null -> listOf(ExpectActualMemberDiff.Kind.NonPrivateCallableAdded)
                else -> expectCallablesWithTheSameName.map {
                    calculateDiffKind(expect = it, actual = unmatchedActualCallable) ?: error("Not equal callables can't have zero diff")
                }
            }.map { kind -> ExpectActualMemberDiff(kind, unmatchedActualCallable.descriptor, expect) }
        }
        .toSet()
}

private fun MemberScope.extractNonPrivateCallables(): Set<Callable> {
    val functions =
        getFunctionNames().asSequence().flatMap { getContributedFunctions(it, NoLookupLocation.FROM_FRONTEND_CHECKER) }
    val properties =
        getVariableNames().asSequence().flatMap { getContributedVariables(it, NoLookupLocation.FROM_FRONTEND_CHECKER) }
    // Cases described in ExpectActualScopeDiff are only possible for non-private methods
    return (functions + properties).filter { !Visibilities.isPrivate(it.visibility.delegate) }
        .map { descriptor ->
            Callable(
                descriptor.name,
                when (descriptor) {
                    is PropertyDescriptor -> Kind.PROPERTY
                    is FunctionDescriptor -> Kind.FUNCTION
                    else -> error("Unknown kind $descriptor")
                },
                descriptor.extensionReceiverParameter?.type,
                descriptor.contextReceiverParameters.map(ValueDescriptor::getType),
                descriptor.valueParameters.map { Parameter(it.name, it.type) },
                descriptor.returnType ?: error("Can't get return type"),
                descriptor.modality,
                descriptor.visibility.delegate,
                descriptor,
            )
        }
        .toSet()
}

private data class Parameter(val name: Name, val type: KotlinType)
private enum class Kind { FUNCTION, PROPERTY }
private class Callable(
    val name: Name,
    val kind: Kind,
    val extensionReceiverType: KotlinType?,
    val contextReceiverTypes: List<KotlinType>,
    val parameters: List<Parameter>,
    val returnType: KotlinType,
    val modality: Modality,
    val visibility: Visibility,
    val descriptor: CallableMemberDescriptor,
) {
    override fun equals(other: Any?): Boolean = other is Callable && calculateDiffKind(this, other) == null
    override fun hashCode(): Int =
        Objects.hash(name, kind, extensionReceiverType, contextReceiverTypes, parameters, returnType, modality, visibility)
}

private val CallableMemberDescriptor.psiIfReal: KtCallableDeclaration?
    get() = takeIf { it.kind.isReal }?.source?.let { it as? KotlinSourceElement }?.psi as? KtCallableDeclaration

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

private val ExpectActualMemberDiff.Kind.factory: DiagnosticFactory1<KtCallableDeclaration, ExpectActualMemberDiff>
    get() = when (this) {
        ExpectActualMemberDiff.Kind.NonPrivateCallableAdded -> Errors.NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ReturnTypeCovariantOverride -> Errors.RETURN_TYPE_COVARIANT_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.ModalityChangedInOverride -> Errors.MODALITY_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
        ExpectActualMemberDiff.Kind.VisibilityChangedInOverride -> Errors.VISIBILITY_OVERRIDE_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION
    }
