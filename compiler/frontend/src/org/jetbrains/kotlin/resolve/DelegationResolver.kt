/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.diagnostics.Errors.MANY_IMPL_MEMBER_NOT_IMPLEMENTED
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

class DelegationResolver<T : CallableMemberDescriptor> private constructor(
        private val classOrObject: KtClassOrObject,
        private val ownerDescriptor: ClassDescriptor,
        private val existingMembers: Collection<CallableDescriptor>,
        private val trace: BindingTrace,
        private val memberExtractor: DelegationResolver.MemberExtractor<T>,
        private val typeResolver: DelegationResolver.TypeResolver
) {

    private fun generateDelegatedMembers(): Collection<T> {
        val delegatedMembers = hashSetOf<T>()
        for (delegationSpecifier in classOrObject.getSuperTypeListEntries()) {
            if (delegationSpecifier !is KtDelegatedSuperTypeEntry) {
                continue
            }
            val typeReference = delegationSpecifier.typeReference ?: continue
            val delegatedInterfaceType = typeResolver.resolve(typeReference)
            if (delegatedInterfaceType == null || delegatedInterfaceType.isError) {
                continue
            }
            val delegatesForInterface = generateDelegatesForInterface(delegatedMembers, delegatedInterfaceType)
            delegatedMembers.addAll(delegatesForInterface)
        }
        return delegatedMembers
    }

    private fun generateDelegatesForInterface(existingDelegates: Collection<T>, delegatedInterfaceType: KotlinType): Collection<T> =
            generateDelegationCandidates(delegatedInterfaceType).filter { candidate ->
                !isOverridingAnyOf(candidate, existingMembers) &&
                !checkClashWithOtherDelegatedMember(candidate, existingDelegates)
            }

    private fun generateDelegationCandidates(delegatedInterfaceType: KotlinType): Collection<T> =
            getDelegatableMembers(delegatedInterfaceType).map { memberDescriptor ->
                val newModality = if (memberDescriptor.modality == Modality.ABSTRACT) Modality.OPEN else memberDescriptor.modality
                @Suppress("UNCHECKED_CAST")
                (memberDescriptor.copy(ownerDescriptor, newModality, Visibilities.INHERITED, DELEGATION, false) as T)
            }

    private fun checkClashWithOtherDelegatedMember(candidate: T, delegatedMembers: Collection<T>): Boolean {
        val alreadyDelegated = delegatedMembers.firstOrNull { isOverridableBy(it, candidate) }
        if (alreadyDelegated != null) {
            trace.report(MANY_IMPL_MEMBER_NOT_IMPLEMENTED.on(classOrObject, classOrObject, alreadyDelegated))
            return true
        }
        return false
    }


    private fun getDelegatableMembers(interfaceType: KotlinType): Collection<T> {
        val classSupertypeMembers =
                TypeUtils.getAllSupertypes(interfaceType).firstOrNull {
                    val typeConstructor = it.constructor.declarationDescriptor
                    typeConstructor is ClassDescriptor && typeConstructor.kind != ClassKind.INTERFACE
                }?.let {
                    memberExtractor.getMembersByType(it)
                } ?: emptyList<CallableMemberDescriptor>()
        return memberExtractor.getMembersByType(interfaceType).filter { descriptor ->
            descriptor.isOverridable && !classSupertypeMembers.any { isOverridableBy(it, descriptor)  }
        }
    }

    interface MemberExtractor<T : CallableMemberDescriptor> {
        fun getMembersByType(type: KotlinType): Collection<T>
    }

    interface TypeResolver {
        fun resolve(reference: KtTypeReference): KotlinType?
    }

    companion object {
        fun <T : CallableMemberDescriptor> generateDelegatedMembers(
                classOrObject: KtClassOrObject,
                ownerDescriptor: ClassDescriptor,
                existingMembers: Collection<CallableDescriptor>,
                trace: BindingTrace,
                memberExtractor: MemberExtractor<T>,
                typeResolver: TypeResolver
        ): Collection<T> =
                DelegationResolver(classOrObject, ownerDescriptor, existingMembers, trace, memberExtractor, typeResolver)
                        .generateDelegatedMembers()

        private fun isOverridingAnyOf(
                candidate: CallableMemberDescriptor,
                possiblyOverriddenBy: Collection<CallableDescriptor>
        ): Boolean =
                possiblyOverriddenBy.any { isOverridableBy(it, candidate) }

        private fun isOverridableBy(memberOne: CallableDescriptor, memberTwo: CallableDescriptor): Boolean =
                OverridingUtil.DEFAULT.isOverridableBy(memberOne, memberTwo, null).result == OVERRIDABLE

    }
}
