/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.serialization

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeAsSequence
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.addToStdlib.cast

class JvmIdSignatureDescriptor(private val mangler: KotlinMangler.DescriptorMangler) : IdSignatureDescriptor(mangler) {

    private class JvmDescriptorBasedSignatureBuilder(mangler: KotlinMangler.DescriptorMangler) : DescriptorBasedSignatureBuilder(mangler) {
        override fun platformSpecificFunction(descriptor: FunctionDescriptor) {
            keepTrackOfOverridesForPossiblyClashingFakeOverride(descriptor)
        }

        override fun platformSpecificProperty(descriptor: PropertyDescriptor) {
            // See KT-31646
            setSpecialJavaProperty(descriptor is JavaForKotlinOverridePropertyDescriptor)
            keepTrackOfOverridesForPossiblyClashingFakeOverride(descriptor)
        }

        override fun platformSpecificGetter(descriptor: PropertyGetterDescriptor) {
            keepTrackOfOverridesForPossiblyClashingFakeOverride(descriptor)
        }

        override fun platformSpecificSetter(descriptor: PropertySetterDescriptor) {
            keepTrackOfOverridesForPossiblyClashingFakeOverride(descriptor)
        }

        private fun keepTrackOfOverridesForPossiblyClashingFakeOverride(descriptor: CallableMemberDescriptor) {
            if (descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) return
            val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return

            val possiblyClashingMembers = when (descriptor) {
                is PropertyAccessorDescriptor ->
                    containingClass.unsubstitutedMemberScope
                        .getContributedVariables(descriptor.correspondingProperty.name, NoLookupLocation.FROM_BACKEND)
                is PropertyDescriptor ->
                    containingClass.unsubstitutedMemberScope
                        .getContributedVariables(descriptor.name, NoLookupLocation.FROM_BACKEND)
                is FunctionDescriptor ->
                    containingClass.unsubstitutedMemberScope
                        .getContributedFunctions(descriptor.name, NoLookupLocation.FROM_BACKEND)
                else ->
                    throw AssertionError("Unexpected CallableMemberDescriptor: $descriptor")
            }
            if (possiblyClashingMembers.size <= 1) return

            val capturingOverrides = descriptor.overriddenTreeAsSequence(true).filter {
                it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE && isCapturingTypeParameter(it)
            }.toList()
            if (capturingOverrides.isNotEmpty()) {
                overridden = capturingOverrides.sortedBy {
                    it.containingDeclaration.cast<ClassDescriptor>().fqNameUnsafe.asString()
                }
            }
        }

        private fun isCapturingTypeParameter(member: CallableMemberDescriptor): Boolean {
            val containingClasses = collectContainingClasses(member)
            return member.extensionReceiverParameter?.isCapturingTypeParameter(containingClasses) == true ||
                    member.valueParameters.any { it.isCapturingTypeParameter(containingClasses) }
        }

        private fun collectContainingClasses(member: CallableMemberDescriptor): Set<ClassDescriptor> {
            val result = HashSet<ClassDescriptor>()
            var pointer: DeclarationDescriptor = member
            while (true) {
                val containingClass = pointer.containingDeclaration as? ClassDescriptor ?: break
                result.add(containingClass)
                if (!containingClass.isInner) break
                pointer = containingClass
            }
            return result
        }

        private fun ParameterDescriptor.isCapturingTypeParameter(containingClasses: Set<ClassDescriptor>): Boolean =
            type.containsTypeParametersOf(containingClasses)

        private fun KotlinType.containsTypeParametersOf(containingClasses: Set<ClassDescriptor>): Boolean =
            contains {
                val descriptor = it.constructor.declarationDescriptor
                descriptor is TypeParameterDescriptor && descriptor.containingDeclaration in containingClasses
            }
    }

    override fun createSignatureBuilder(): DescriptorBasedSignatureBuilder = JvmDescriptorBasedSignatureBuilder(mangler)
}