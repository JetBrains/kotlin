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
import org.jetbrains.kotlin.config.LanguageFeature
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
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement
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

        checkInheritedAndDelegatedSignatures(classDescriptor, inheritedMemberErrors, inheritedMemberErrors)
        inheritedMemberErrors.doReportErrors()
    }

    private interface CheckInheritedSignaturesReportStrategy {
        fun abstractMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractBaseClassMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun abstractMemberWithMoreSpecificType(abstractMember: CallableMemberDescriptor, concreteMember: CallableMemberDescriptor)
        fun multipleImplementationsMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun conflictingInterfaceMemberNotImplemented(descriptor: CallableMemberDescriptor)
        fun typeMismatchOnInheritance(descriptor1: CallableMemberDescriptor, descriptor2: CallableMemberDescriptor)
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

        override fun abstractMemberWithMoreSpecificType(
            abstractMember: CallableMemberDescriptor,
            concreteMember: CallableMemberDescriptor
        ) {
            shouldImplement.add(abstractMember)
        }
    }

    private inner class CollectErrorInformationForInheritedMembersStrategy(
        private val klass: KtClassOrObject,
        private val classDescriptor: ClassDescriptor
    ) : CheckInheritedSignaturesReportStrategy, CheckOverrideReportStrategy {

        private val abstractNoImpl = linkedSetOf<CallableMemberDescriptor>()
        private val abstractInBaseClassNoImpl = linkedSetOf<CallableMemberDescriptor>()
        private val multipleImplementations = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingInterfaceMembers = linkedSetOf<CallableMemberDescriptor>()
        private val conflictingReturnTypes = linkedSetOf<CallableMemberDescriptor>()

        private val onceErrorsReported = SmartHashSet<DiagnosticFactoryWithPsiElement<*, *>>()

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
            reportDelegationProblemIfRequired(
                RETURN_TYPE_MISMATCH_BY_DELEGATION, RETURN_TYPE_MISMATCH_ON_INHERITANCE, overriding, overridden
            )
        }

        override fun propertyTypeMismatchOnOverride(
            overriding: PropertyDescriptor,
            overridden: PropertyDescriptor
        ) {
            reportDelegationProblemIfRequired(
                PROPERTY_TYPE_MISMATCH_BY_DELEGATION, PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, overriding, overridden
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

            if (!onceErrorsReported.contains(diagnosticFactory) && (relevantDiagnosticFromInheritance == null || !onceErrorsReported.contains(
                    relevantDiagnosticFromInheritance
                ))) {
                onceErrorsReported.add(diagnosticFactory)
                trace.report(diagnosticFactory.on(klass, delegate, overridden))
            }
        }

        internal fun doReportErrors() {
            val canHaveAbstractMembers = classCanHaveAbstractFakeOverride(classDescriptor)
            if (abstractInBaseClassNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractInBaseClassNoImpl.first()))
            } else if (abstractNoImpl.isNotEmpty() && !canHaveAbstractMembers) {
                trace.report(ABSTRACT_MEMBER_NOT_IMPLEMENTED.on(klass, klass, abstractNoImpl.first()))
            }

            conflictingInterfaceMembers.removeAll(conflictingReturnTypes)
            multipleImplementations.removeAll(conflictingReturnTypes)
            if (!conflictingInterfaceMembers.isEmpty()) {
                trace.report(MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED.on(klass, klass, conflictingInterfaceMembers.iterator().next()))
            } else if (!multipleImplementations.isEmpty()) {
                trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(klass, klass, multipleImplementations.iterator().next()))
            }
        }
    }

    private interface CheckOverrideReportStrategy {
        fun overridingFinalMember(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun returnTypeMismatchOnOverride(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor)
        fun propertyTypeMismatchOnOverride(overriding: PropertyDescriptor, overridden: PropertyDescriptor)
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
                ?: throw IllegalStateException("declared descriptor is not resolved to declaration: " + declared)

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
                        trace.report(RETURN_TYPE_MISMATCH_ON_OVERRIDE.on(
                            member, declared, DeclarationWithDiagnosticComponents(overridden, platformSpecificDiagnosticComponents)
                        ))
                    }
                }

                override fun propertyTypeMismatchOnOverride(overriding: PropertyDescriptor, overridden: PropertyDescriptor) {
                    if (!typeMismatchError) {
                        typeMismatchError = true
                        if (overridden.isVar) {
                            trace.report(VAR_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden))
                        } else {
                            trace.report(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE.on(member, declared, overridden))
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
            })
        } else if (!overriddenDescriptors.isEmpty() && !overridesBackwardCompatibilityHelper.overrideCanBeOmitted(declared)) {
            val overridden = overriddenDescriptors.iterator().next()
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

            override fun propertyTypeMismatchOnOverride(overriding: PropertyDescriptor, overridden: PropertyDescriptor) {
                throw IllegalStateException("Component functions are not properties")
            }

            override fun varOverriddenByVal(overriding: CallableMemberDescriptor, overridden: CallableMemberDescriptor) {
                throw IllegalStateException("Component functions are not properties")
            }
        })
    }

    private fun checkOverrideForCopyFunction(copyFunction: CallableMemberDescriptor) {
        val overridden = copyFunction.overriddenDescriptors.firstOrNull()
        if (overridden != null) {
            val baseClassifier = overridden.containingDeclaration
            val dataModifier = findDataModifierForDataClass(copyFunction.containingDeclaration)
            if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitDataClassesOverridingCopy)) {
                trace.report(DATA_CLASS_OVERRIDE_DEFAULT_VALUES_ERROR.on(dataModifier, copyFunction, baseClassifier))
            } else {
                trace.report(DATA_CLASS_OVERRIDE_DEFAULT_VALUES_WARNING.on(dataModifier, copyFunction, baseClassifier))
            }
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
                ?: error("Declaration not found for parameter: " + descriptor)

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
                ?: error("Declaration not found for class: " + containingClass)

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
            val compare = Visibilities.compare(visibility, descriptor.visibility)
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
            return fun(descriptor: CallableMemberDescriptor): Unit {
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
                return Unit
            }
        }

        fun getMissingImplementations(classDescriptor: ClassDescriptor): Set<CallableMemberDescriptor> {
            val collector = CollectMissingImplementationsStrategy()
            checkInheritedAndDelegatedSignatures(classDescriptor, collector, null)
            return collector.shouldImplement
        }

        private fun checkInheritedAndDelegatedSignatures(
            classDescriptor: ClassDescriptor,
            inheritedReportStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?
        ) {
            for (member in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (member is CallableMemberDescriptor) {
                    checkInheritedAndDelegatedSignatures(member, inheritedReportStrategy, overrideReportStrategyForDelegates)
                }
            }
        }

        private fun checkInheritedAndDelegatedSignatures(
            descriptor: CallableMemberDescriptor,
            reportingStrategy: CheckInheritedSignaturesReportStrategy,
            overrideReportStrategyForDelegates: CheckOverrideReportStrategy?
        ) {
            val kind = descriptor.kind
            if (kind != FAKE_OVERRIDE && kind != DELEGATION) return
            if (descriptor.visibility === Visibilities.INVISIBLE_FAKE) return

            val directOverridden = descriptor.overriddenDescriptors
            assert(!directOverridden.isEmpty()) { kind.toString() + " " + descriptor.name.asString() + " must override something" }

            // collects map from the directly overridden descriptor to the set of declarations:
            // -- if directly overridden is not fake, the set consists of one element: this directly overridden
            // -- if it's fake, overridden declarations (non-fake) of this descriptor are collected
            val overriddenDeclarationsByDirectParent = collectOverriddenDeclarations(directOverridden)

            val allOverriddenDeclarations = ContainerUtil.flatten(overriddenDeclarationsByDirectParent.values)
            val allFilteredOverriddenDeclarations = OverridingUtil.filterOutOverridden(
                Sets.newLinkedHashSet(allOverriddenDeclarations)
            )

            val relevantDirectlyOverridden =
                getRelevantDirectlyOverridden(overriddenDeclarationsByDirectParent, allFilteredOverriddenDeclarations)

            checkInheritedDescriptorsGroup(descriptor, relevantDirectlyOverridden, reportingStrategy)

            if (kind == DELEGATION && overrideReportStrategyForDelegates != null) {
                checkOverridesForMember(descriptor, relevantDirectlyOverridden, overrideReportStrategyForDelegates)
            }

            if (kind != DELEGATION) {
                checkMissingOverridesByJava8Restrictions(relevantDirectlyOverridden, reportingStrategy)
            }

            val (concreteOverridden, abstractOverridden) = relevantDirectlyOverridden
                .filter { !isOrOverridesSynthesized(it) }
                .partition { it.modality != Modality.ABSTRACT }

            val numImplementations = concreteOverridden.size

            when (numImplementations) {
                0 ->
                    if (kind != DELEGATION) {
                        abstractOverridden.forEach {
                            reportingStrategy.abstractMemberNotImplemented(it)
                        }
                    }
                1 ->
                    if (kind != DELEGATION) {
                        val implementation = concreteOverridden.first()
                        collectAbstractMethodsWithMoreSpecificReturnType(abstractOverridden, implementation).forEach {
                            reportingStrategy.abstractMemberWithMoreSpecificType(it, implementation)
                        }
                    }
                else ->
                    concreteOverridden.forEach {
                        reportingStrategy.multipleImplementationsMemberNotImplemented(it)
                    }
            }
        }

        private fun checkMissingOverridesByJava8Restrictions(
            relevantDirectlyOverridden: Set<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy
        ) {
            // Java 8:
            // -- class should implement an abstract member of a super-class,
            //    even if relevant default implementation is provided in one of the super-interfaces;
            // -- inheriting multiple override equivalent methods from an interface is a conflict
            //    regardless of 'default' vs 'abstract'.

            var overridesClassMember = false
            var overridesNonAbstractInterfaceMember = false
            var overridesAbstractInBaseClass: CallableMemberDescriptor? = null
            val overriddenInterfaceMembers = SmartList<CallableMemberDescriptor>()
            for (overridden in relevantDirectlyOverridden) {
                val containingDeclaration = overridden.containingDeclaration
                if (containingDeclaration is ClassDescriptor) {
                    if (containingDeclaration.kind == ClassKind.CLASS) {
                        overridesClassMember = true
                        if (overridden.modality === Modality.ABSTRACT) {
                            overridesAbstractInBaseClass = overridden
                        }
                    } else if (containingDeclaration.kind == ClassKind.INTERFACE) {
                        overriddenInterfaceMembers.add(overridden)
                        if (overridden.modality !== Modality.ABSTRACT) {
                            overridesNonAbstractInterfaceMember = true
                        }
                    }
                }
            }

            if (overridesAbstractInBaseClass != null) {
                reportingStrategy.abstractBaseClassMemberNotImplemented(overridesAbstractInBaseClass)
            }
            if (!overridesClassMember && overridesNonAbstractInterfaceMember && overriddenInterfaceMembers.size > 1) {
                for (member in overriddenInterfaceMembers) {
                    reportingStrategy.conflictingInterfaceMemberNotImplemented(member)
                }
            }
        }

        private fun collectAbstractMethodsWithMoreSpecificReturnType(
            abstractOverridden: List<CallableMemberDescriptor>,
            implementation: CallableMemberDescriptor
        ): List<CallableMemberDescriptor> =
            abstractOverridden.filter { abstractMember -> !isReturnTypeOkForOverride(abstractMember, implementation) }

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
                overriddenDeclarationsByDirectParent.put(descriptor, LinkedHashSet(filteredOverrides))
            }
            return overriddenDeclarationsByDirectParent
        }

        private fun checkInheritedDescriptorsGroup(
            descriptor: CallableMemberDescriptor,
            overriddenDescriptors: Collection<CallableMemberDescriptor>,
            reportingStrategy: CheckInheritedSignaturesReportStrategy
        ) {
            if (overriddenDescriptors.size <= 1) return

            val propertyDescriptor = descriptor as? PropertyDescriptor

            for (overriddenDescriptor in overriddenDescriptors) {
                if (propertyDescriptor != null) {
                    val overriddenPropertyDescriptor =
                        overriddenDescriptor.assertedCast<PropertyDescriptor> { "$overriddenDescriptor is not a property" }
                    if (!isPropertyTypeOkForOverride(overriddenPropertyDescriptor, propertyDescriptor)) {
                        reportingStrategy.typeMismatchOnInheritance(propertyDescriptor, overriddenPropertyDescriptor)
                    }
                } else {
                    if (!isReturnTypeOkForOverride(overriddenDescriptor, descriptor)) {
                        reportingStrategy.typeMismatchOnInheritance(descriptor, overriddenDescriptor)
                    }
                }
            }
        }

        private fun checkOverridesForMemberMarkedOverride(
            declared: CallableMemberDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner,
            reportError: CheckOverrideReportForDeclaredMemberStrategy
        ) {
            val overriddenDescriptors = declared.overriddenDescriptors

            checkOverridesForMember(declared, overriddenDescriptors, reportError)

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
            reportError: CheckOverrideReportStrategy
        ) {
            val propertyMemberDescriptor = if (memberDescriptor is PropertyDescriptor) memberDescriptor else null

            for (overridden in overriddenDescriptors) {
                if (overridden.modality == Modality.FINAL) {
                    reportError.overridingFinalMember(memberDescriptor, overridden)
                }

                if (propertyMemberDescriptor != null) {
                    val overriddenProperty = overridden.assertedCast<PropertyDescriptor> {
                        "$overridden is overridden by property $propertyMemberDescriptor"
                    }
                    if (!isPropertyTypeOkForOverride(overriddenProperty, propertyMemberDescriptor)) {
                        reportError.propertyTypeMismatchOnOverride(propertyMemberDescriptor, overriddenProperty)
                    }
                } else if (!isReturnTypeOkForOverride(overridden, memberDescriptor)) {
                    reportError.returnTypeMismatchOnOverride(memberDescriptor, overridden)
                }

                if (checkPropertyKind(overridden, true) && checkPropertyKind(memberDescriptor, false)) {
                    reportError.varOverriddenByVal(memberDescriptor, overridden)
                }
            }
        }

        private fun isReturnTypeOkForOverride(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor
        ): Boolean {
            val typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor) ?: return false

            val superReturnType = superDescriptor.returnType!!

            val subReturnType = subDescriptor.returnType!!

            val substitutedSuperReturnType = typeSubstitutor.substitute(superReturnType, Variance.OUT_VARIANCE)!!

            return KotlinTypeChecker.DEFAULT.isSubtypeOf(subReturnType, substitutedSuperReturnType)
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

        private fun isPropertyTypeOkForOverride(
            superDescriptor: PropertyDescriptor,
            subDescriptor: PropertyDescriptor
        ): Boolean {
            val typeSubstitutor = prepareTypeSubstitutor(superDescriptor, subDescriptor) ?: return false

            val substitutedSuperReturnType = typeSubstitutor.substitute(superDescriptor.type, Variance.OUT_VARIANCE)!!

            return if (superDescriptor.isVar) {
                KotlinTypeChecker.DEFAULT.equalTypes(subDescriptor.type, substitutedSuperReturnType)
            } else {
                KotlinTypeChecker.DEFAULT.isSubtypeOf(subDescriptor.type, substitutedSuperReturnType)
            }
        }

        private fun findDataModifierForDataClass(dataClass: DeclarationDescriptor): PsiElement {
            val classDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(dataClass) as KtClassOrObject?
            if (classDeclaration?.modifierList != null) {
                val modifier = classDeclaration.modifierList!!.getModifier(KtTokens.DATA_KEYWORD)
                if (modifier != null) {
                    return modifier
                }
            }

            throw IllegalStateException("No data modifier is found for data class " + dataClass)
        }

        private fun findInvisibleOverriddenDescriptor(
            declared: CallableMemberDescriptor,
            declaringClass: ClassDescriptor,
            kotlinTypeRefiner: KotlinTypeRefiner
        ): CallableMemberDescriptor? {
            @UseExperimental(TypeRefinement::class)
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
