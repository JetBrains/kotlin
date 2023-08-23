/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object ActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val (actual, expect) = matchActualWithNonFinalExpect(declaration, descriptor, context) ?: return

        // The explicit casts won't be necessary when we start compiling kotlin with K2. K1 doesn't build CFG properly
        declaration as KtClassLikeDeclaration
        descriptor as ClassifierDescriptorWithTypeParameters

        checkSupertypes(expect, actual, context, declaration, descriptor)
        checkExpectActualScopeDiff(expect, actual, context, declaration, descriptor)
    }
}

private fun checkSupertypes(
    expect: ClassDescriptor,
    actual: ClassDescriptor,
    context: DeclarationCheckerContext,
    declaration: KtClassLikeDeclaration,
    descriptor: ClassifierDescriptorWithTypeParameters,
) {
    val addedSupertypes = (actual.getSuperInterfaces() + listOfNotNull(actual.getSuperClassNotAny())).map(ClassDescriptor::fqNameSafe) -
            (expect.getSuperInterfaces() + listOfNotNull(expect.getSuperClassNotAny())).map(ClassDescriptor::fqNameSafe).toSet()

    if (addedSupertypes.isNotEmpty()) {
        context.trace.report(
            Errors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_SUPERTYPES_AS_NON_FINAL_EXPECT_CLASSIFIER.on(
                declaration,
                descriptor,
                addedSupertypes.map(FqName::shortName),
                expect,
            )
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
            Errors.ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER.on(
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

    if (!descriptor.isActual) return null

    with(OptInUsageChecker) {
        if (declaration.isDeclarationAnnotatedWith(allowDifferentMembersInActualFqn, context.trace.bindingContext)) return null
    }

    val actual = when (descriptor) {
        is ClassDescriptor -> descriptor
        is TypeAliasDescriptor -> descriptor.classDescriptor
        else -> error("ClassifierDescriptorWithTypeParameters has only two inheritors")
    } ?: return null
    // If actual is final then expect is final as well (otherwise another checker will report a diagnostic).
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
): Set<ExpectActualMemberDiff<CallableMemberDescriptor, ClassDescriptor>> {
    val matchingContext = ClassicExpectActualMatchingContext(actual.module)
    val classTypeSubstitutor = matchingContext.createExpectActualTypeParameterSubstitutor(
        expect.declaredTypeParameters,
        actual.declaredTypeParameters,
        parentSubstitutor = null
    )

    val expectClassCallables = expect.unsubstitutedMemberScope
        .extractNonPrivateCallables(classTypeSubstitutor, ExpectActual.EXPECT, matchingContext)
    val actualClassCallables = actual.unsubstitutedMemberScope
        .extractNonPrivateCallables(classTypeSubstitutor, ExpectActual.ACTUAL, matchingContext)
        .filter { it.descriptor.kind.isReal } // Filter out fake-overrides from actual because we compare list of supertypes separately anyway

    val nameAndKindToExpectCallable = expectClassCallables.groupBy { it.name to it.kind }

    return (actualClassCallables - expectClassCallables).asSequence()
        .flatMap { unmatchedActualCallable ->
            when (val expectCallablesWithTheSameNameAndKind =
                nameAndKindToExpectCallable[unmatchedActualCallable.name to unmatchedActualCallable.kind]) {
                null -> listOf(ExpectActualMemberDiff.Kind.NonPrivateCallableAdded)
                else -> expectCallablesWithTheSameNameAndKind.map {
                    calculateExpectActualMemberDiffKind(
                        expect = it,
                        actual = unmatchedActualCallable,
                        checkParameterNames = unmatchedActualCallable.descriptor.hasStableParameterNames()
                    )
                }
            }.filterNotNull().map { kind -> ExpectActualMemberDiff(kind, unmatchedActualCallable.descriptor, expect) }
        }
        .toSet()
}

private fun MemberScope.extractNonPrivateCallables(
    classTypeSubstitutor: TypeSubstitutorMarker,
    expectActual: ExpectActual,
    matchingContext: ClassicExpectActualMatchingContext,
): Set<Callable> {
    val functions =
        getFunctionNames().asSequence().flatMap { getContributedFunctions(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }
    val properties =
        getVariableNames().asSequence().flatMap { getContributedVariables(it, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS) }
    return (functions + properties).filter { !Visibilities.isPrivate(it.visibility.delegate) }
        .map { descriptor -> Callable(descriptor, expectActual, classTypeSubstitutor, matchingContext) }
        .toSet()
}

private data class Parameter(val name: Name, val type: KotlinType)
private enum class Kind { FUNCTION, PROPERTY }
private enum class ExpectActual { EXPECT, ACTUAL }
private data class TypeParameter(val name: Name, val upperBounds: List<KotlinType>)
private class Callable(
    val descriptor: CallableMemberDescriptor,
    val expectActual: ExpectActual,
    val classTypeSubstitutor: TypeSubstitutorMarker,
    val matchingContext: ClassicExpectActualMatchingContext,
) {
    val name: Name = descriptor.name
    val kind: Kind = when (descriptor) {
        is PropertyDescriptor -> Kind.PROPERTY
        is FunctionDescriptor -> Kind.FUNCTION
        else -> error("Unknown kind $descriptor")
    }
    val isVarProperty: Boolean = descriptor is PropertyDescriptor && descriptor.isVar
    val isLateinitProperty: Boolean = descriptor is PropertyDescriptor && descriptor.isLateInit
    val modality: Modality = descriptor.modality
    val visibility: Visibility = descriptor.visibility.delegate
    val setterVisibility: Visibility? = (descriptor as? PropertyDescriptor)?.setter?.visibility?.delegate
    val parameters: List<Parameter> = descriptor.valueParameters.map { Parameter(it.name, it.type) }
    val returnType: KotlinType = descriptor.returnType ?: error("Can't get return type")
    val extensionReceiverType: KotlinType? = descriptor.extensionReceiverParameter?.type
    val typeParameters: List<TypeParameter> = descriptor.typeParameters.map { TypeParameter(it.name, it.upperBounds) }
    val contextReceiverTypes: List<KotlinType> = descriptor.contextReceiverParameters.map(ValueDescriptor::getType)

    override fun equals(other: Any?): Boolean {
        if (other !is Callable) return false
        check(classTypeSubstitutor === other.classTypeSubstitutor)
        check(matchingContext === other.matchingContext)
        return if (expectActual == other.expectActual) {
            name == other.name &&
                    kind == other.kind &&
                    isVarProperty == other.isVarProperty &&
                    isLateinitProperty == other.isLateinitProperty &&
                    modality == other.modality &&
                    visibility == other.visibility &&
                    setterVisibility == other.setterVisibility &&
                    parameters == other.parameters &&
                    returnType == other.returnType &&
                    extensionReceiverType == other.extensionReceiverType &&
                    typeParameters == other.typeParameters &&
                    contextReceiverTypes == other.contextReceiverTypes
        } else {
            val (expect, actual) = if (expectActual == ExpectActual.EXPECT) this to other else other to this
            calculateExpectActualMemberDiffKind(expect, actual) == null
        }
    }

    override fun hashCode(): Int = // Don't hash the types because type comparison is complicated
        Objects.hash(
            name,
            kind,
            isVarProperty,
            isLateinitProperty,
            modality,
            visibility,
            setterVisibility,
            parameters.map(Parameter::name),
            extensionReceiverType != null,
            typeParameters.map(TypeParameter::name),
            contextReceiverTypes.size,
        )
}

private val CallableMemberDescriptor.psiIfReal: KtCallableDeclaration?
    get() = takeIf { it.kind.isReal }?.source?.let { it as? KotlinSourceElement }?.psi as? KtCallableDeclaration

private fun ClassicExpectActualMatchingContext.areCompatibleWithSubstitution(
    expect: KotlinType?,
    actual: KotlinType?,
    substitutor: TypeSubstitutorMarker,
): Boolean = areCompatibleExpectActualTypes(expect?.let { substitutor.safeSubstitute(it) }, actual)

private fun ClassicExpectActualMatchingContext.areCompatibleListWithSubstitution(
    expect: List<KotlinType>,
    actual: List<KotlinType>,
    substitutor: TypeSubstitutorMarker,
): Boolean = expect.size == actual.size && expect.asSequence().zip(actual.asSequence())
    .all { (a, b) -> areCompatibleWithSubstitution(a, b, substitutor) }

private fun ClassicExpectActualMatchingContext.areCompatibleUpperBoundsWithSubstitution(
    expect: List<List<KotlinType>>,
    actual: List<List<KotlinType>>,
    substitutor: TypeSubstitutorMarker,
): Boolean = expect.size == actual.size && expect.asSequence().zip(actual.asSequence())
    .all { (a, b) -> areCompatibleListWithSubstitution(a, b, substitutor) }

private fun calculateExpectActualMemberDiffKind(
    expect: Callable,
    actual: Callable,
    checkParameterNames: Boolean = true,
): ExpectActualMemberDiff.Kind? {
    check(expect.expectActual == ExpectActual.EXPECT)
    check(actual.expectActual == ExpectActual.ACTUAL)
    check(expect.classTypeSubstitutor === actual.classTypeSubstitutor)
    check(expect.matchingContext === actual.matchingContext)
    val substitutor = actual.matchingContext.createExpectActualTypeParameterSubstitutor(
        expect.descriptor.typeParameters,
        actual.descriptor.typeParameters,
        actual.classTypeSubstitutor
    )
    with(actual.matchingContext) {
        return when {
            expect.name != actual.name ||
                    expect.kind != actual.kind ||
                    !areCompatibleListWithSubstitution(
                        expect.parameters.map(Parameter::type),
                        actual.parameters.map(Parameter::type),
                        substitutor
                    ) ||
                    !areCompatibleUpperBoundsWithSubstitution(
                        expect.typeParameters.map(TypeParameter::upperBounds),
                        actual.typeParameters.map(TypeParameter::upperBounds),
                        substitutor
                    ) ||
                    !areCompatibleWithSubstitution(
                        expect.extensionReceiverType,
                        actual.extensionReceiverType,
                        substitutor
                    ) ||
                    !areCompatibleListWithSubstitution(
                        expect.contextReceiverTypes,
                        actual.contextReceiverTypes,
                        substitutor
                    ) ->
                ExpectActualMemberDiff.Kind.NonPrivateCallableAdded

            expect.isVarProperty != actual.isVarProperty -> ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride

            expect.isLateinitProperty != actual.isLateinitProperty -> ExpectActualMemberDiff.Kind.LateinitChangedInOverride

            expect.modality != actual.modality -> ExpectActualMemberDiff.Kind.ModalityChangedInOverride

            expect.visibility != actual.visibility -> ExpectActualMemberDiff.Kind.VisibilityChangedInOverride

            expect.setterVisibility != actual.setterVisibility -> ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride

            checkParameterNames && expect.parameters.map(Parameter::name) != actual.parameters.map(Parameter::name) ->
                ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride

            expect.typeParameters.map(TypeParameter::name) != actual.typeParameters.map(TypeParameter::name) ->
                ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride

            !areCompatibleWithSubstitution(expect.returnType, actual.returnType, substitutor) ->
                ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride

            else -> null
        }
    }
}

private fun BindingTrace.reportIfPossible(diff: ExpectActualMemberDiff<CallableMemberDescriptor, ClassDescriptor>) {
    val psi = diff.actualMember.psiIfReal ?: return
    val diagnostic = when (diff.kind) {
        ExpectActualMemberDiff.Kind.NonPrivateCallableAdded ->
            Errors.NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.ReturnTypeChangedInOverride ->
            Errors.RETURN_TYPE_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.ModalityChangedInOverride ->
            Errors.MODALITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.VisibilityChangedInOverride ->
            Errors.VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.SetterVisibilityChangedInOverride ->
            Errors.SETTER_VISIBILITY_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on((psi as? KtProperty)?.setter ?: return, diff)
        ExpectActualMemberDiff.Kind.ParameterNameChangedInOverride ->
            Errors.PARAMETER_NAME_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.PropertyKindChangedInOverride ->
            Errors.PROPERTY_KIND_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.LateinitChangedInOverride ->
            Errors.LATEINIT_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
        ExpectActualMemberDiff.Kind.TypeParameterNamesChangedInOverride ->
            Errors.TYPE_PARAMETER_NAMES_CHANGED_IN_NON_FINAL_EXPECT_CLASSIFIER_ACTUALIZATION.on(psi, diff)
    }
    report(diagnostic)
}
