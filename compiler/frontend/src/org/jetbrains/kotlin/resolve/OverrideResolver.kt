/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.SmartHashSet
import org.jetbrains.kotlin.config.LanguageFeature.AbstractClassMemberNotImplementedWithIntermediateAbstractClass
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactoryWithPsiElement
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.rendering.DeclarationWithDiagnosticComponents
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.platform.PlatformSpecificDiagnosticComponents
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils.classCanHaveAbstractFakeOverride
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.isOrOverridesSynthesized
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewKotlinTypeCheckerImpl
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.utils.addToStdlib.assertedCast
import java.util.*

class OverrideResolver(
    private val trace: BindingTrace,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper,
    private val languageVersionSettings: LanguageVersionSettings,
    private val kotlinTypeRefiner: KotlinTypeRefiner,
    private val platformSpecificDiagnosticComponents: PlatformSpecificDiagnosticComponents
) {

    fun check(c: TopDownAnalysisContext) {
        checkVisibility(c)
        checkOverrides(c)
        checkParameterOverridesForAllClasses(c)
    }


    private fun checkOverrides(c: TopDownAnalysisContext) {
        for ((key, value) in c.declaredClasses) {
            checkOverridesInAClass(value, key)
        }
    }

    private fun checkOverridesInAClass(classDescriptor: ClassDescriptorWithResolutionScopes, klass: KtClassOrObject) {
        // Check overrides for internal consistency
        for (member in classDescriptor.declaredCallableMembers) {
            checkOverrideForMember(member)
        }

        val inheritedMemberErrors = CollectErrorInformationForInheritedMembersStrategy(klass, classDescriptor)

        checkInheritedAndDelegatedSignatures(classDescriptor, inheritedMemberErrors, inheritedMemberErrors, kotlinTypeRefiner)
        inheritedMemberErrors.doReportErrors()
    }

    private interface CheckInheritedSignaturesReportStrategy {
        fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractMemberWithMoreSpecificType(abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor)
        fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun typeMismatchOnInheritance(descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor)
        fun abstractInvisibleMember(descriptor: CallableMemberDescriptor)
    }

    private class CollectMissingImplementationsStrategy : CheckInheritedSignaturesReportStrategy {
        val shouldImplement = LinkedHashSet<CallableMemberDescriptor>()

        override fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            shouldImplement.add(descriptor)
        }

        override fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            // don't care
        }

        override fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            shouldImplement.add(descriptor)
        }

        override fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            if (descriptor.modality === Modality.ABSTRACT) {
                shouldImplement.add(descriptor)
            }
        }

        override fun typeMismatchOnInheritance(descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor) {
            // don't care
        }

        override fun abstractInvisibleMember(descriptor: CallableMemberDescriptor) {
            // don't care
        }

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor,
            concreteMember: CallableMemberDescriptor
        ) {
            shouldImplement.add(abstractMember)
        }
    }

    private inner class CollectWarningInformationForInheritedMembersStrategy(
        klass: KtClassOrObject,
        classDescriptor: ClassDescriptor
    ) : CollectErrorInformationForInheritedMembersStrategy(klass, classDescriptor) {
        constructor(delegateStrategy: CollectErrorInformationForInheritedMembersStrategy) :
                this(delegateStrategy.klass, delegateStrategy.classDescriptor)

        override fun doReportErrors() {
            val canHaveAbstractMembers = classCanHaveAbstractFakeOverride(classDescriptor)
            if (abstractInBaseClassNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                if (languageVersionSettings.supportsFeature(AbstractClassMemberNotImplementedWithIntermediateAbstractClass)) {
                    trace.report(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractInBaseClassNoImpl.first()))
                } else {
                    trace.report(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED_WARNING.on(klass, klass, abstractInBaseClassNoImpl.first()))
                }
            }
            if (conflictingInterfaceMembers.isNotEmpty()) {
                val interfaceMember = conflictingInterfaceMembers.first()
                if (languageVersionSettings.supportsFeature(AbstractClassMemberNotImplementedWithIntermediateAbstractClass)) {
                    trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(klass, klass, interfaceMember))
                } else {
                    trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING.on(klass, klass, interfaceMember))
                }
            }
        }
    }

    private open inner class CollectErrorInformationForInheritedMembersStrategy(
        val klass: KtClassOrObject,
        val classDescriptor: ClassDescriptor
    ) : CheckInheritedSignaturesReportStrategy, CheckOverrideReportStrategy {

        private val abstractNoImpl = linkedSetOf<CallableMemberDescriptor>()
        protected val abstractInBaseClassNoImpl = linkedSetOf<CallableMemberDescriptor>()
        private val abstractInvisibleSuper = linkedSetOf<CallableMemberDescriptor>()
        private val multipleImplementations = linkedSetOf<CallableMemberDescriptor>()
        protected val conflictingInterfaceMembers = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingReturnTypes = linkedSetOf<CallableMemberDescriptor>()

        private val onceErrorsReported = SmartHashSet<DiagnosticFactoryWithPsiElement<*, *>>()

        fun toDeprecationStrategy() =
            CollectWarningInformationForInheritedMembersStrategy(this)

        override fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            abstractNoImpl.add(descriptor)
        }

        override fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            abstractInBaseClassNoImpl.add(descriptor)
        }

        override fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            multipleImplementations.add(descriptor)
        }

        override fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor) {
            conflictingInterfaceMembers.add(descriptor)
        }

        override fun typeMismatchOnInheritance(descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor) {
            conflictingReturnTypes.add(descriptor1)
            conflictingReturnTypes.add(descriptor2)

            if (descriptor1 is PropertyDescriptor && descriptor2 is PropertyDescriptor) {
                if (descriptor1.isVar || descriptor2.isVar) {
                    reportInheritanceConflictIfRequired(VAR_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                } else {
                    reportInheritanceConflictIfRequired(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
                }
            } else {
                reportInheritanceConflictIfRequired(RETURN_TYPE_MISMATCH_ON_INHERITANCE, descriptor1, descriptor2)
            }
        }

        override fun abstractInvisibleMember(descriptor: CallableMemberDescriptor) {
            abstractInvisibleSuper.add(descriptor)
        }

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor,
            concreteMember: CallableMemberDescriptor
        ) {
            typeMismatchOnInheritance(abstractMember, concreteMember)
        }

        private fun reportInheritanceConflictIfRequired(
            diagnosticFactory: DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor>,
            descriptor1: CallableMemberDescriptor,
            descriptor2: CallableMemberDescriptor
        ) {
            if (!onceErrorsReported.contains(diagnosticFactory)) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(klass, descriptor1, descriptor2))
            }
        }

        override fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
            reportDelegationProblemIfRequired(OVERRIDING_FINAL_MEMBER_BY_DELEGATION, null, overriding, overridden)
        }

        override fun returnTypeMismatchOnOverride(
            overriding: CallableMemberDescriptor,
            overridden: CallableMemberDescriptor
        ) {
            val (diagnosticFactory, relevantDiagnosticFromInheritance) = if (overridden is PropertyDescriptor)
                PROPERTY_TYPE_MISMATCH_BY_DELEGATION to PROPERTY_TYPE_MISMATCH_ON_INHERITANCE
            else
                RETURN_TYPE_MISMATCH_BY_DELEGATION to RETURN_TYPE_MISMATCH_ON_INHERITANCE

            reportDelegationProblemIfRequired(
                diagnosticFactory, relevantDiagnosticFromInheritance, overriding, overridden
            )
        }

        override fun varOverriddenByVal(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
            reportDelegationProblemIfRequired(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, null, overriding, overridden)
        }

        private fun reportDelegationProblemIfRequired(
            diagnosticFactory: DiagnosticFactory2<KtClassOrObject, CallableMemberDescriptor, CallableMemberDescriptor>,
            relevantDiagnosticFromInheritance: DiagnosticFactoryWithPsiElement<*, *>?,
            delegate: CallableMemberDescriptor,
            overridden: CallableMemberDescriptor
        ) {
            assert(delegate.kind == DELEGATION) { "Delegate expected, got " + delegate + " of kind " + delegate.kind }

            if (!onceErrorsReported.contains(diagnosticFactory) &&
                (relevantDiagnosticFromInheritance == null || !onceErrorsReported.contains(relevantDiagnosticFromInheritance))
            ) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(klass, delegate, overridden))
            }
        }

        open fun doReportErrors() {
            val canHaveAbstractMembers = classCanHaveAbstractFakeOverride(classDescriptor)
            if (abstractInBaseClassNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractInBaseClassNoImpl.first()))
            } else if (abstractNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractNoImpl.first()))
            }

            if (abstractInvisibleSuper.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(INVISIBLE_ABSTRACT_MEMBER_FROM_SUPER.on(languageVersionSettings, klass, classDescriptor, abstractInvisibleSuper))
            }

            conflictingInterfaceMembers.removeAll(conflictingReturnTypes)
            multipleImplementations.removeAll(conflictingReturnTypes)
            if (conflictingInterfaceMembers.isNotEmpty()) {
                trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(klass, klass, conflictingInterfaceMembers.first()))
            } else if (multipleImplementations.isNotEmpty()) {
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(klass, klass, multipleImplementations.first()))
            }
        }
    }

    private interface CheckOverrideReportStrategy {
        fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun returnTypeMismatchOnOverride(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun varOverriddenByVal(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
    }

    private interface CheckOverrideReportForDeclaredMemberStrategy : CheckOverrideReportStrategy {
        fun nothingToOverride(overriding: CallableMemberDescriptor)
        fun cannotOverrideInvisibleMember(overriding: CallableMemberDescriptor, invisibleOverridden: CallableMemberDescriptor)
    }

    private fun checkOverrideForMember(declared: CallableMemberDescriptor) {
        if (declared.kind == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            if (DataClassDescriptorResolver.isComponentLike(declared.name)) {
                checkOverrideForComponentFunction(declared)
            } else if (declared.name == DataClassDescriptorResolver.COPY_METHOD_NAME) {
                checkOverrideForCopyFunction(declared)
            }
            return
        }

        if (declared.kind != CallableMemberDescriptor.Kind.DECLARATION) {
            return
        }

        val member = DescriptorToSourceUtils.descriptorToDeclaration(declared) as KtNamedDeclaration?
            ?: throw IllegalStateException("declared descriptor is not resolved to declaration: $declared")

        val modifierList = member.modifierList
        val hasOverrideNode = modifierList != null && modifierList.hasModifier(KtTokens.OVERRIDE_KEYWORD)
        val overriddenDescriptors = declared.overriddenDescriptors

        if (hasOverrideNode) {
            checkOverridesForMemberMarkedOverride(
                declared, kotlinTypeRefiner, object : CheckOverrideReportForDeclaredMemberStrategy {
                    private var finalOverriddenError = false
                    private var typeMismatchError = false
                    private var kindMismatchError = false

                    override fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                        if (!finalOverriddenError) {
                            finalOverriddenError = true
                            trace.report(OVERRIDING_FINAL_MEMBER.on(member, overridden, overridden.containingDeclaration))
                        }
                    }

                    override fun returnTypeMismatchOnOverride(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                        if (!typeMismatchError) {
                            typeMismatchError = true

                            when {
                                overridden is PropertyDescriptor && overridden.isVar ->
                                    trace.report(VAR_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden))

                                overridden is PropertyDescriptor && !overridden.isVar ->
                                    trace.report(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden))

                                else -> trace.report(
                                    RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(
                                        member, declared,
                                        DeclarationWithDiagnosticComponents(overridden, platformSpecificDiagnosticComponents)
                                    )
                                )
                            }
                        }
                    }

                    override fun varOverriddenByVal(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                        if (!kindMismatchError) {
                            kindMismatchError = true
                            trace.report(VAR_OVERRIDDEN_BY_VAL.on(member, declared as PropertyDescriptor, overridden as PropertyDescriptor))
                        }
                    }

                    override fun cannotOverrideInvisibleMember(
                        overriding: CallableMemberDescriptor,
                        invisibleOverridden: CallableMemberDescriptor
                    ) {
                        trace.report(CANNOT_OVERRIDE_INVISIBLE_MEMBER.on(member, declared, invisibleOverridden))
                    }

                    override fun nothingToOverride(overriding: CallableMemberDescriptor) {
                        trace.report(NOTHING_TO_OVERRIDE.on(member, declared))
                    }
                }
            )
        } else if (!overriddenDescriptors.isEmpty() && !overridesBackwardCompatibilityHelper.overrideCanBeOmitted(declared)) {
            val overridden = overriddenDescriptors.first()
            trace.report(VIRTUAL_MEMBER_HIDDEN.on(member, declared, overridden, overridden.containingDeclaration))
        }
    }

    private fun checkOverrideForComponentFunction(componentFunction: CallableMemberDescriptor) {
        val dataModifier = findDataModifierForDataClass(componentFunction.containingDeclaration)

        checkOverridesForMember(componentFunction, componentFunction.overriddenDescriptors, object : CheckOverrideReportStrategy {
            private var overrideConflict = false

            override fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                if (!overrideConflict) {
                    overrideConflict = true
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataModifier, componentFunction, overridden.containingDeclaration))
                }
            }

            override fun returnTypeMismatchOnOverride(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                if (!overrideConflict) {
                    overrideConflict = true
                    trace.report(DATA_CLASS_OVERRIDE_CONFLICT.on(dataModifier, componentFunction, overridden.containingDeclaration))
                }
            }

            override fun varOverriddenByVal(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                throw IllegalStateException("Component functions are not properties")
            }
        }, kotlinTypeRefiner)
    }

    private fun checkOverrideForCopyFunction(copyFunction: CallableMemberDescriptor) {
        val overridden = copyFunction.overriddenDescriptors.firstOrNull()
        if (overridden != null) {
            val baseClassifier = overridden.containingDeclaration
            val dataModifier = findDataModifierForDataClass(copyFunction.containingDeclaration)
            trace.report(DATA_CLASS_OVERRIDE_DEFAULT_VALUES.on(languageVersionSettings, dataModifier, copyFunction, baseClassifier))
        }
    }

    private fun checkParameterOverridesForAllClasses(c: TopDownAnalysisContext) {
        for (classDescriptor in c.declaredClasses.values) {
            for (member in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (member is CallableMemberDescriptor) {
                    checkOverridesForParameters(member)
                }
            }
        }
    }

    private fun checkOverridesForParameters(declared: CallableMemberDescriptor) {
        val isDeclaration = declared.kind == CallableMemberDescriptor.Kind.DECLARATION
        if (isDeclaration) {
            // No check if the function is not marked as 'override'
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(declared) as KtModifierListOwner?
            if (declaration != null && !declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
                return
            }
        }

        // Let p1 be a parameter of the overriding function
        // Let p2 be a parameter of the function being overridden
        // Then
        //  a) p1 is not allowed to have a default value declared
        //  b) p1 must have the same name as p2
        for (parameterFromSubclass in declared.valueParameters) {
            var defaultsInSuper = 0
            for (parameterFromSuperclass in parameterFromSubclass.overriddenDescriptors) {
                if (parameterFromSuperclass.declaresDefaultValue()) {
                    defaultsInSuper++
                }
            }
            val multipleDefaultsInSuper = defaultsInSuper > 1

            if (isDeclaration) {
                checkNameAndDefaultForDeclaredParameter(parameterFromSubclass, multipleDefaultsInSuper)
            } else {
                checkNameAndDefaultForFakeOverrideParameter(declared, parameterFromSubclass, multipleDefaultsInSuper)
            }
        }
    }

    private fun checkNameAndDefaultForDeclaredParameter(descriptor: ValueParameterDescriptor, multipleDefaultsInSuper: Boolean) {
        val parameter = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtParameter
            ?: error("Declaration not found for parameter: $descriptor")

        if (descriptor.declaresDefaultValue()) {
            trace.report(DEFAULT_VALUE_NOT_ALLOWED_IN_OVERRIDE.on(parameter))
        }

        if (multipleDefaultsInSuper) {
            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES.on(parameter, descriptor))
        }

        for (parameterFromSuperclass in descriptor.overriddenDescriptors) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {

                trace.report(
                    PARAMETER_NAME_CHANGED_ON_OVERRIDE.on(
                        parameter,
                        parameterFromSuperclass.containingDeclaration.containingDeclaration as ClassDescriptor,
                        parameterFromSuperclass
                    )
                )
            }
        }
    }

    private fun checkNameAndDefaultForFakeOverrideParameter(
        containingFunction: CallableMemberDescriptor,
        descriptor: ValueParameterDescriptor,
        multipleDefaultsInSuper: Boolean
    ) {
        val containingClass = containingFunction.containingDeclaration
        val classElement = DescriptorToSourceUtils.descriptorToDeclaration(containingClass) as KtClassOrObject?
            ?: error("Declaration not found for class: $containingClass")

        if (multipleDefaultsInSuper) {
            trace.report(MULTIPLE_DEFAULTS_INHERITED_FROM_SUPERTYPES_WHEN_NO_EXPLICIT_OVERRIDE.on(classElement, descriptor))
        }

        for (parameterFromSuperclass in descriptor.overriddenDescriptors) {
            if (shouldReportParameterNameOverrideWarning(descriptor, parameterFromSuperclass)) {
                trace.report(
                    DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES.on(
                        classElement,
                        containingFunction.overriddenDescriptors,
                        parameterFromSuperclass.index + 1
                    )
                )
            }
        }
    }

    private fun checkVisibility(c: TopDownAnalysisContext) {
        for ((key, value) in c.members) {
            checkVisibilityForMember(key, value)
            if (key is KtProperty && value is PropertyDescriptor) {
                val setter = key.setter
                val setterDescriptor = value.setter
                if (setter != null && setterDescriptor != null) {
                    checkVisibilityForMember(setter, setterDescriptor)
                }
            }
        }
    }

    private fun checkVisibilityForMember(declaration: KtDeclaration, memberDescriptor: CallableMemberDescriptor) {
        val visibility = memberDescriptor.visibility
        for (descriptor in memberDescriptor.overriddenDescriptors) {
            val compare = DescriptorVisibilities.compare(visibility, descriptor.visibility)
            if (compare == null) {
                trace.report(
                    CANNOT_CHANGE_ACCESS_PRIVILEGE.on(
                        declaration,
                        descriptor.visibility,
                        descriptor,
                        descriptor.containingDeclaration
                    )
                )
                return
            } else if (compare < 0) {
                trace.report(
                    CANNOT_WEAKEN_ACCESS_PRIVILEGE.on(
                        declaration,
                        descriptor.visibility,
                        descriptor,
                        descriptor.containingDeclaration
                    )
                )
                return
            }
        }
    }

    companion object {

        fun resolveUnknownVisibilities(
            descriptors: Collection<CallableMemberDescriptor>,
            trace: BindingTrace
        ) {
            for (descriptor in descriptors) {
                OverridingUtil.resolveUnknownVisibilityForMember(descriptor, createCannotInferVisibilityReporter(trace))
            }
        }

        fun createCannotInferVisibilityReporter(trace: BindingTrace): Function1<CallableMemberDescriptor, Unit> {
            return fun(descriptor: CallableMemberDescriptor) {
                val reportOn: DeclarationDescriptor = when {
                    descriptor.kind == FAKE_OVERRIDE || descriptor.kind == DELEGATION ->
                        DescriptorUtils.getContainingClass(descriptor) ?: throw AssertionError("Class member expected: $descriptor")
                    descriptor is PropertyAccessorDescriptor && descriptor.isDefault ->
                        descriptor.correspondingProperty
                    else ->
                        descriptor
                }

                val element = DescriptorToSourceUtils.descriptorToDeclaration(reportOn)
                if (element is KtDeclaration) {
                    trace.report(CANNOT_INFER_VISIBILITY.on(element, descriptor))
                }
                return
            }
        }

        fun getMissingImplementations(classDescriptor: ClassDescriptor): Set<CallableMemberDescriptor> {
            val collector = CollectMissingImplementationsStrategy()
            // Note that it is fine to pass default refiner here. Reason:
            // 1. We bind overrides with proper refiners and [checkInheritedAndDelegatedSignatures] skips all properly-bound overrides,
            //    so we would consider only unbound overrides
            // 2. Using default refiner instead of proper one can only increase amount of type mismatches, not decrease it
            // Putting 1 and 2 together means that using default refiner might make already unbound overrides even "more unbound", which
            // isn't an issue for case of [getMissingImplementations]
            checkInheritedAndDelegatedSignatures(classDescriptor, collector, null, KotlinTypeRefiner.Default)
            return collector.shouldImplement
        }

        private fun checkInheritedAndDelegatedSignatures(
            classDescriptor: ClassDescriptor,
            inheritedReportStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?,
            kotlinTypeRefiner: KotlinTypeRefiner
        ) {
            for (member in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (member is CallableMemberDescriptor) {
                    checkInheritedAndDelegatedSignatures(
                        member, inheritedReportStrategy, overrideReportStrategyForDelegates, kotlinTypeRefiner
                    )
                }
            }
        }

        private fun CallableMemberDescriptor.computeRelevantDirectlyOverridden(): Set<CallableMemberDescriptor> {
            val directOverridden = overriddenDescriptors

            // directOverridden may be empty if user tries to delegate implementation of abstract class instead of interface
            if (directOverridden.isEmpty()) return emptySet()

            // collects map from the directly overridden descriptor to the set of declarations:
            // -- if directly overridden is not fake, the set consists of one element: this directly overridden
            // -- if it's fake, overridden declarations (non-fake) of this descriptor are collected
            val overriddenDeclarationsByDirectParent = collectOverriddenDeclarations(directOverridden)

            val allOverriddenDeclarations = ContainerUtil.flatten(overriddenDeclarationsByDirectParent.values)
            val allFilteredOverriddenDeclarations = OverridingUtil.filterOutOverridden(
                Sets.newLinkedHashSet(allOverriddenDeclarations)
            )

            return getRelevantDirectlyOverridden(overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations)
        }

        private fun checkInheritedAndDelegatedSignatures(
            descriptor: CallableMemberDescriptor,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?,
            kotlinTypeRefiner: KotlinTypeRefiner
        ) {
            val kind = descriptor.kind
            if (kind != FAKE_OVERRIDE && kind != DELEGATION) return

            val relevantDirectlyOverridden = descriptor.computeRelevantDirectlyOverridden()
            if (relevantDirectlyOverridden.isEmpty()) return

            if (descriptor.visibility === DescriptorVisibilities.INVISIBLE_FAKE) {
                checkInvisibleFakeOverride(descriptor, relevantDirectlyOverridden, reportingStrategy)
                return
            }

            checkInheritedDescriptorsGroup(descriptor, relevantDirectlyOverridden, reportingStrategy, kotlinTypeRefiner)

            if (kind == DELEGATION && overrideReportStrategyForDelegates != null) {
                checkOverridesForMember(descriptor, relevantDirectlyOverridden, overrideReportStrategyForDelegates, kotlinTypeRefiner)
            }

            if (kind != DELEGATION) {
                checkMissingOverridesByJava8Restrictions(relevantDirectlyOverridden, reportingStrategy)
            }

            val (concreteOverridden, abstractOverridden) = relevantDirectlyOverridden
                .filter { !isOrOverridesSynthesized(it) }
                .partition { it.modality != Modality.ABSTRACT }

            when (concreteOverridden.size) {
                0 ->
                    if (kind != DELEGATION) {
                        abstractOverridden.forEach {
                            reportingStrategy.abstractMemberNotImplemented(it)
                        }
                    }
                1 ->
                    if (kind != DELEGATION) {
                        val implementation = concreteOverridden.first()
                        collectAbstractMethodsWithMoreSpecificReturnType(abstractOverridden, implementation, kotlinTypeRefiner).forEach {
                            reportingStrategy.abstractMemberWithMoreSpecificType(it, implementation)
                        }
                    }
                else ->
                    concreteOverridden.forEach {
                        reportingStrategy.multipleImplementationsMemberNotImplemented(it)
                    }
            }
        }

        private fun checkInvisibleFakeOverride(
            descriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy
        ) {
            // the checks below are only relevant for non-abstract classes or objects
            if ((descriptor.containingDeclaration as? ClassDescriptor)?.modality === Modality.ABSTRACT) return

            val abstractOverrides = overriddenDescriptors.filter { it.modality === Modality.ABSTRACT }

            if (abstractOverrides.size != overriddenDescriptors.size) return // has non-abstract override

            for (override in abstractOverrides) {
                reportingStrategy.abstractInvisibleMember(override)
            }
        }

        private fun checkMissingOverridesByJava8Restrictions(
            relevantDirectlyOverridden: Set<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            onlyBaseClassMembers: Boolean = false,
            overriddenInterfaceMembers: MutableList<CallableMemberDescriptor> = SmartList()
        ) {
            // Java 8:
            // -- class should implement an abstract member of a super-class,
            //    even if relevant default implementation is provided in one of the super-interfaces;
            // -- inheriting multiple override equivalent methods from an interface is a conflict
            //    regardless of 'default' vs 'abstract'.

            var overridesClassMember = false
            var overridesNonAbstractInterfaceMember = false
            var overridesAbstractInBaseClass: CallableMemberDescriptor? = null
            var overridesNonAbstractInBaseClass: CallableMemberDescriptor? = null
            var fakeOverrideInBaseClass: CallableMemberDescriptor? = null
            for (overridden in relevantDirectlyOverridden) {
                val containingDeclaration = overridden.containingDeclaration as? ClassDescriptor ?: continue
                if (containingDeclaration.kind == ClassKind.CLASS) {
                    if (overridden.kind == FAKE_OVERRIDE && !containingDeclaration.isExpect) {
                        // Fake override in a class in fact can mean an interface member
                        // We will process it at the end
                        // Note: with expect containing class, the situation is unclear, so we miss this case
                        // See extendExpectedClassWithAbstractMember.kt (BaseA, BaseAImpl, DerivedA1)
                        fakeOverrideInBaseClass = overridden
                    }
                    overridesClassMember = true
                    if (overridden.modality === Modality.ABSTRACT) {
                        overridesAbstractInBaseClass = overridden
                    } else {
                        overridesNonAbstractInBaseClass = overridden
                    }
                } else if (containingDeclaration.kind == ClassKind.INTERFACE) {
                    overriddenInterfaceMembers.add(overridden)
                    if (overridden.modality !== Modality.ABSTRACT) {
                        overridesNonAbstractInterfaceMember = true
                    }
                }
            }

            if (overridesAbstractInBaseClass != null && overridesNonAbstractInBaseClass == null) {
                reportingStrategy.abstractBaseClassMemberNotImplemented(overridesAbstractInBaseClass)
            } else if (!onlyBaseClassMembers && !overridesClassMember &&
                overridesNonAbstractInterfaceMember && overriddenInterfaceMembers.size > 1
            ) {
                for (member in overriddenInterfaceMembers) {
                    reportingStrategy.conflictingInterfaceMemberNotImplemented(member)
                }
            } else if (fakeOverrideInBaseClass != null) {
                val newReportingStrategy = if (reportingStrategy is CollectErrorInformationForInheritedMembersStrategy) {
                    reportingStrategy.toDeprecationStrategy()
                } else reportingStrategy
                checkMissingOverridesByJava8Restrictions(
                    fakeOverrideInBaseClass.computeRelevantDirectlyOverridden(),
                    reportingStrategy = newReportingStrategy,
                    // Note: we don't report MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING
                    // in case all interface members are derived from fakeOverrideInClass
                    // (in this case we already have warning or error on its container)
                    onlyBaseClassMembers = overriddenInterfaceMembers.isEmpty(),
                    overriddenInterfaceMembers
                )
                if (newReportingStrategy is CollectWarningInformationForInheritedMembersStrategy) {
                    newReportingStrategy.doReportErrors()
                }
            }
        }

        private fun collectAbstractMethodsWithMoreSpecificReturnType(
            abstractOverridden: List<CallableMemberDescriptor>,
            implementation: CallableMemberDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner
        ): List<CallableMemberDescriptor> =
            abstractOverridden.filter { abstractMember -> !isReturnTypeOkForOverride(abstractMember, implementation, kotlinTypeRefiner) }

        private fun getRelevantDirectlyOverridden(
            overriddenByParent: MutableMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>>,
            allFilteredOverriddenDeclarations: Set<CallableMemberDescriptor>
        ): Set<CallableMemberDescriptor> {
            /* Let the following class hierarchy is declared:

        trait A { fun foo() = 1 }
        trait B : A
        trait C : A
        trait D : A { override fun foo() = 2 }
        trait E : B, C, D {}

        Traits B and C have fake descriptors for function foo.
        The map 'overriddenByParent' is:
        { 'foo in B' (fake) -> { 'foo in A' }, 'foo in C' (fake) -> { 'foo in A' }, 'foo in D' -> { 'foo in D'} }
        This is a map from directly overridden descriptors (functions 'foo' in B, C, D in this example) to the set of declarations (non-fake),
        that are overridden by this descriptor.

        The goal is to leave only relevant directly overridden descriptors to count implementations of our fake function on them.
        In the example above there is no error (trait E inherits only one implementation of 'foo' (from D), because this implementation is more precise).
        So only 'foo in D' is relevant.

        Directly overridden descriptor is not relevant if it doesn't add any more appropriate non-fake declarations of the concerned function.
        More precisely directly overridden descriptor is not relevant if:
        - it's declaration set is a subset of declaration set for other directly overridden descriptor
        ('foo in B' is not relevant because it's declaration set is a subset of 'foo in C' function's declaration set)
        - each member of it's declaration set is overridden by a member of other declaration set
        ('foo in C' is not relevant, because 'foo in A' is overridden by 'foo in D', so 'foo in A' is not appropriate non-fake declaration for 'foo')

        For the last condition allFilteredOverriddenDeclarations helps (for the example above it's { 'foo in A' } only): each declaration set
        is compared with allFilteredOverriddenDeclarations, if they have no intersection, this means declaration set has only functions that
        are overridden by some other function and corresponding directly overridden descriptor is not relevant.
        */

            val iterator = overriddenByParent.entries.iterator()
            while (iterator.hasNext()) {
                if (!isRelevant(iterator.next().value, overriddenByParent.values, allFilteredOverriddenDeclarations)) {
                    iterator.remove()
                }
            }
            return overriddenByParent.keys
        }

        private fun isRelevant(
            declarationSet: Set<CallableMemberDescriptor>,
            allDeclarationSets: Collection<Set<CallableMemberDescriptor>>,
            allFilteredOverriddenDeclarations: Set<CallableMemberDescriptor>
        ): Boolean {
            for (otherSet in allDeclarationSets) {
                if (otherSet === declarationSet) continue
                if (otherSet.containsAll(declarationSet)) return false
                if (Collections.disjoint(allFilteredOverriddenDeclarations, declarationSet)) return false
            }
            return true
        }

        private fun collectOverriddenDeclarations(
            directOverriddenDescriptors: Collection<CallableMemberDescriptor>
        ): MutableMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>> {
            val overriddenDeclarationsByDirectParent = Maps.newLinkedHashMap<CallableMemberDescriptor, Set<CallableMemberDescriptor>>()
            for (descriptor in directOverriddenDescriptors) {
                val overriddenDeclarations = OverridingUtil.getOverriddenDeclarations(descriptor)
                val filteredOverrides = OverridingUtil.filterOutOverridden(overriddenDeclarations)
                overriddenDeclarationsByDirectParent[descriptor] = LinkedHashSet(filteredOverrides)
            }
            return overriddenDeclarationsByDirectParent
        }

        private fun checkInheritedDescriptorsGroup(
            descriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            kotlinTypeRefiner: KotlinTypeRefiner
        ) {
            if (overriddenDescriptors.size <= 1) return

            for (overriddenDescriptor in overriddenDescriptors) {
                require(descriptor !is PropertyDescriptor || overriddenDescriptor is PropertyDescriptor) {
                    "$overriddenDescriptor is not a property"
                }

                if (!isReturnTypeOkForOverride(overriddenDescriptor, descriptor, kotlinTypeRefiner)) {
                    reportingStrategy.typeMismatchOnInheritance(descriptor, overriddenDescriptor)
                }
            }
        }

        private fun checkOverridesForMemberMarkedOverride(
            declared: CallableMemberDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner,
            reportError: CheckOverrideReportForDeclaredMemberStrategy
        ) {
            val overriddenDescriptors = declared.overriddenDescriptors

            checkOverridesForMember(declared, overriddenDescriptors, reportError, kotlinTypeRefiner)

            if (overriddenDescriptors.isEmpty()) {
                val containingDeclaration = declared.containingDeclaration
                val declaringClass = containingDeclaration.assertedCast<ClassDescriptor> {
                    "Overrides may only be resolved in a class, but $declared comes from $containingDeclaration"
                }

                val invisibleOverriddenDescriptor =
                    findInvisibleOverriddenDescriptor(
                        declared, declaringClass, kotlinTypeRefiner
                    )
                if (invisibleOverriddenDescriptor != null) {
                    reportError.cannotOverrideInvisibleMember(declared, invisibleOverriddenDescriptor)
                } else {
                    reportError.nothingToOverride(declared)
                }
            }
        }

        private fun checkOverridesForMember(
            memberDescriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportError: CheckOverrideReportStrategy,
            kotlinTypeRefiner: KotlinTypeRefiner
        ) {
            for (overridden in overriddenDescriptors) {
                if (overridden.modality == Modality.FINAL) {
                    reportError.overridingFinalMember(memberDescriptor, overridden)
                }

                if (!isReturnTypeOkForOverride(overridden, memberDescriptor, kotlinTypeRefiner)) {
                    require(memberDescriptor !is PropertyDescriptor || overridden is PropertyDescriptor) {
                        "$overridden is overridden by property $memberDescriptor"
                    }
                    reportError.returnTypeMismatchOnOverride(memberDescriptor, overridden)
                }

                if (checkPropertyKind(overridden, true) && checkPropertyKind(memberDescriptor, false)) {
                    reportError.varOverriddenByVal(memberDescriptor, overridden)
                }
            }
        }

        private fun isReturnTypeOkForOverride(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner,
        ): Boolean {
            val typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor) ?: return false

            val superReturnType = superDescriptor.returnType!!

            val subReturnType = subDescriptor.returnType!!

            val substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType, Variance.OUT_VARIANCE)!!

            val typeChecker = NewKotlinTypeCheckerImpl(kotlinTypeRefiner)
            return if (superDescriptor is PropertyDescriptor && superDescriptor.isVar)
                typeChecker.equalTypes(subReturnType, substitutedSuperReturnType)
            else
                typeChecker.isSubtypeOf(subReturnType, substitutedSuperReturnType)
        }

        private fun prepareTypeSubstitutor(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): TypeSubstitutor? {
            val superTypeParameters = superDescriptor.typeParameters
            val subTypeParameters = subDescriptor.typeParameters
            if (subTypeParameters.size != superTypeParameters.size) return null

            val arguments = ArrayList<TypeProjection>(subTypeParameters.size)
            for (i in superTypeParameters.indices) {
                arguments.add(TypeProjectionImpl(subTypeParameters[i].defaultType))
            }

            return IndexedParametersSubstitution(superTypeParameters, arguments).buildSubstitutor()
        }

        private fun findDataModifierForDataClass(dataClass: DeclarationDescriptor): PsiElement {
            val classDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(dataClass) as KtClassOrObject?
            if (classDeclaration?.modifierList != null) {
                val modifier = classDeclaration.modifierList!!.getModifier(KtTokens.DATA_KEYWORD)
                if (modifier != null) {
                    return modifier
                }
            }

            throw IllegalStateException("No data modifier is found for data class $dataClass")
        }

        private fun findInvisibleOverriddenDescriptor(
            declared: CallableMemberDescriptor,
            declaringClass: ClassDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner
        ): CallableMemberDescriptor? {
            @OptIn(TypeRefinement::class)
            for (supertype in kotlinTypeRefiner.refineSupertypes(declaringClass)) {
                val all = linkedSetOf<CallableMemberDescriptor>()
                all.addAll(supertype.memberScope.getContributedFunctions(declared.name, NoLookupLocation.WHEN_CHECK_OVERRIDES))
                all.addAll(supertype.memberScope.getContributedVariables(declared.name, NoLookupLocation.WHEN_CHECK_OVERRIDES))
                for (fromSuper in all) {
                    if (OverridingUtil.DEFAULT.isOverridableBy(fromSuper, declared, null).result == OVERRIDABLE) {
                        if (OverridingUtil.isVisibleForOverride(declared, fromSuper)) {
                            throw IllegalStateException(
                                "Descriptor " + fromSuper + " is overridable by " + declared +
                                        " and visible but does not appear in its getOverriddenDescriptors()"
                            )
                        }
                        return fromSuper
                    }
                }
            }
            return null
        }

        fun shouldReportParameterNameOverrideWarning(
            parameterFromSubclass: ValueParameterDescriptor,
            parameterFromSuperclass: ValueParameterDescriptor
        ): Boolean {
            return parameterFromSubclass.containingDeclaration.hasStableParameterNames() &&
                    parameterFromSuperclass.containingDeclaration.hasStableParameterNames() &&
                    parameterFromSuperclass.name != parameterFromSubclass.name
        }

        private fun checkPropertyKind(descriptor: CallableMemberDescriptor, isVar: Boolean): Boolean {
            return descriptor is PropertyDescriptor && descriptor.isVar == isVar
        }
    }
}
