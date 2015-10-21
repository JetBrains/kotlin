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
    public @JvmStatic fun isOverloadable(a: CallableDescriptor, b: CallableDescriptor): Boolean {
        val abc = braceCount(a)
        val bbc = braceCount(b)

        if (abc != bbc) {
            return true
        }

        val receiverAndParameterResult = OverridingUtil.checkReceiverAndParameterCount(a, b)
        if (receiverAndParameterResult != null) {
            return receiverAndParameterResult.result == INCOMPATIBLE
        }

        val aValueParameters = OverridingUtil.compiledValueParameters(a)
        val bValueParameters = OverridingUtil.compiledValueParameters(b)

        for (i in aValueParameters.indices) {
            val superValueParameterType = OverridingUtil.getUpperBound(aValueParameters[i])
            val subValueParameterType = OverridingUtil.getUpperBound(bValueParameters[i])
            if (!KotlinTypeChecker.DEFAULT.equalTypes(superValueParameterType, subValueParameterType) ||
                    oneMoreSpecificThanAnother(subValueParameterType, superValueParameterType)) {
                return true
            }
        }

        return false
    }

    private fun braceCount(a: CallableDescriptor): Int =
            when (a) {
                is PropertyDescriptor -> 0
                is SimpleFunctionDescriptor -> 1
                is ConstructorDescriptor -> 1
                else -> throw IllegalStateException()
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

        // TODO handle constructor redeclarations in modules. See also: https://youtrack.jetbrains.com/issue/KT-3632
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

    private fun MemberDescriptor.isPrivate() = Visibilities.isPrivate(this.visibility)

    public @JvmStatic fun getPossibleRedeclarationGroups(members: Collection<CallableMemberDescriptor>): Collection<Collection<CallableMemberDescriptor>> {
        val result = arrayListOf<Collection<CallableMemberDescriptor>>()

        val nonPrivates = members.filter { !it.isPrivate() }
        if (nonPrivates.size > 1) {
            result.add(nonPrivates)
        }

        val bySourceFile = MultiMap.createSmart<SourceFile, CallableMemberDescriptor>()
        for (member in members) {
            val sourceFile = DescriptorUtils.getContainingSourceFile(member)
            if (sourceFile != SourceFile.NO_SOURCE_FILE) {
                bySourceFile.putValue(sourceFile, member)
            }
        }

        for ((sourceFile, membersInFile) in bySourceFile.entrySet()) {
            // File member groups are interesting in redeclaration check if at least one file member is private.
            if (membersInFile.size > 1 && membersInFile.any { it.isPrivate() }) {
                result.add(membersInFile)
            }
        }

        return result
    }
}
