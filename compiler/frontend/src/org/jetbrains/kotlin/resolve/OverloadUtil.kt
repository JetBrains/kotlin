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

import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil.OverrideCompatibilityInfo.Result.*
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.oneMoreSpecificThanAnother

object OverloadUtil {

    /**
     * Does not check names.
     */
    public @JvmStatic fun isOverloadable(a: CallableDescriptor, b: CallableDescriptor): OverloadCompatibilityInfo {
        val abc = braceCount(a)
        val bbc = braceCount(b)

        if (abc != bbc) {
            return OverloadCompatibilityInfo.success()
        }

        val overrideCompatibilityInfo = isOverloadableBy(a, b)
        when (overrideCompatibilityInfo.result) {
            OVERRIDABLE, CONFLICT -> return OverloadCompatibilityInfo.someError()
            INCOMPATIBLE -> return OverloadCompatibilityInfo.success()
            else -> throw IllegalStateException()
        }
    }

    private fun isOverloadableBy(
            superDescriptor: CallableDescriptor,
            subDescriptor: CallableDescriptor): OverridingUtil.OverrideCompatibilityInfo {
        val receiverAndParameterResult = OverridingUtil.checkReceiverAndParameterCount(superDescriptor, subDescriptor)
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult
        }

        val superValueParameters = OverridingUtil.compiledValueParameters(superDescriptor)
        val subValueParameters = OverridingUtil.compiledValueParameters(subDescriptor)

        for (i in superValueParameters.indices) {
            val superValueParameterType = OverridingUtil.getUpperBound(superValueParameters[i])
            val subValueParameterType = OverridingUtil.getUpperBound(subValueParameters[i])
            if (!KotlinTypeChecker.DEFAULT.equalTypes(superValueParameterType, subValueParameterType) || oneMoreSpecificThanAnother(subValueParameterType, superValueParameterType)) {
                return OverridingUtil.OverrideCompatibilityInfo.valueParameterTypeMismatch(superValueParameterType, subValueParameterType, INCOMPATIBLE)
            }
        }

        return OverridingUtil.OverrideCompatibilityInfo.success()
    }

    private fun braceCount(a: CallableDescriptor): Int =
            when (a) {
                is PropertyDescriptor -> 0
                is SimpleFunctionDescriptor -> 1
                is ConstructorDescriptor -> 1
                else -> throw IllegalStateException()
            }

    class OverloadCompatibilityInfo(val isSuccess: Boolean, val message: String) {
        companion object {

            private val SUCCESS = OverloadCompatibilityInfo(true, "SUCCESS")

            fun success() = SUCCESS

            fun someError() = OverloadCompatibilityInfo(false, "XXX")
        }
    }

    public @JvmStatic fun groupModulePackageMembersByFqName(
            c: BodiesResolveContext,
            constructorsInPackages: MultiMap<FqNameUnsafe, ConstructorDescriptor>
    ): MultiMap<FqNameUnsafe, CallableMemberDescriptor> {
        val packageMembersByName = MultiMap<FqNameUnsafe, CallableMemberDescriptor>()

        collectModulePackageMembersWithSameName(packageMembersByName, c.functions.values) {
            scope, name ->
            scope.getFunctions(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
        }

        collectModulePackageMembersWithSameName(packageMembersByName, c.properties.values) {
            scope, name ->
            scope.getProperties(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS).filterIsInstance<CallableMemberDescriptor>()
        }

        // TODO handle constructor redeclarations in modules. See also https://youtrack.jetbrains.com/issue/KT-3632
        packageMembersByName.putAllValues(constructorsInPackages)

        return packageMembersByName
    }

    private inline fun collectModulePackageMembersWithSameName(
            packageMembersByName: MultiMap<FqNameUnsafe, CallableMemberDescriptor>,
            interestingDescriptors: Collection<CallableMemberDescriptor>,
            getMembersByName: (KtScope, Name) -> Collection<CallableMemberDescriptor>
    ) {
        val observedFQNs = hashSetOf<FqNameUnsafe>()
        for (descriptor in interestingDescriptors) {
            if (descriptor.containingDeclaration !is PackageFragmentDescriptor) continue

            val descriptorFQN = DescriptorUtils.getFqName(descriptor)
            if (observedFQNs.contains(descriptorFQN)) continue
            observedFQNs.add(descriptorFQN)

            val packageMembersWithSameName = getModulePackageMembersWithSameName(descriptor, getMembersByName)
            packageMembersByName.putValues(descriptorFQN, packageMembersWithSameName)
        }
    }

    private inline fun getModulePackageMembersWithSameName(
            packageMember: CallableMemberDescriptor,
            getMembersByName: (KtScope, Name) -> Collection<CallableMemberDescriptor>
    ): Collection<CallableMemberDescriptor> {
        val containingPackage = packageMember.containingDeclaration
        if (containingPackage !is PackageFragmentDescriptor) {
            throw AssertionError("$packageMember is not a top-level package member")
        }

        val containingModule = DescriptorUtils.getContainingModuleOrNull(packageMember) ?: return listOf(packageMember)

        val containingPackageScope = containingModule.getPackage(containingPackage.fqName).memberScope
        val possibleOverloads = getMembersByName(containingPackageScope, packageMember.name)

        // NB memberScope for PackageViewDescriptor includes module dependencies
        return possibleOverloads.filter { DescriptorUtils.getContainingModule(it) == containingModule }
    }

}
