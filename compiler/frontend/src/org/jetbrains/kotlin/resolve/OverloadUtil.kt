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
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SpecificityComparisonCallbacks
import org.jetbrains.kotlin.resolve.calls.results.isSignatureNotLessSpecific
import org.jetbrains.kotlin.resolve.calls.results.varargParameterPosition
import org.jetbrains.kotlin.resolve.descriptorUtil.hasLowPriorityInOverloadResolution
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.singletonOrEmptyList

object OverloadUtil {
    /**
     * Does not check names.
     */
    @JvmStatic fun isOverloadable(a: DeclarationDescriptor, b: DeclarationDescriptor): Boolean {
        val aCategory = getDeclarationCategory(a)
        val bCategory = getDeclarationCategory(b)

        if (aCategory != bCategory) return true
        if (a !is CallableDescriptor || b !is CallableDescriptor) return false

        return checkOverloadability(a, b)
    }

    private object OverloadabilitySpecificityCallbacks : SpecificityComparisonCallbacks {
        override fun isNonSubtypeNotLessSpecific(specific: KotlinType, general: KotlinType): Boolean =
                false
    }

    private fun checkOverloadability(a: CallableDescriptor, b: CallableDescriptor): Boolean {
        if (a.hasLowPriorityInOverloadResolution() != b.hasLowPriorityInOverloadResolution()) return true

        // NB this makes generic and non-generic declarations with equivalent signatures non-conflicting
        // E.g., 'fun <T> foo()' and 'fun foo()'.
        // They can be disambiguated by providing explicit type parameters.
        if (a.typeParameters.isEmpty() != b.typeParameters.isEmpty()) return true

        if (ErrorUtils.containsErrorType(a) || ErrorUtils.containsErrorType(b)) return true
        if (a.varargParameterPosition() != b.varargParameterPosition()) return true

        val aSignature = FlatSignature.createFromCallableDescriptor(a)
        val bSignature = FlatSignature.createFromCallableDescriptor(b)

        val aIsNotLessSpecificThanB = isSignatureNotLessSpecific(aSignature, bSignature, OverloadabilitySpecificityCallbacks)
        val bIsNotLessSpecificThanA = isSignatureNotLessSpecific(bSignature, aSignature, OverloadabilitySpecificityCallbacks)

        return !(aIsNotLessSpecificThanB && bIsNotLessSpecificThanA)
    }

    private enum class DeclarationCategory {
        TYPE_OR_VALUE,
        FUNCTION,
        EXTENSION_PROPERTY
    }

    private fun DeclarationDescriptor.isExtensionProperty() =
            this is PropertyDescriptor &&
            extensionReceiverParameter != null

    private fun getDeclarationCategory(a: DeclarationDescriptor): DeclarationCategory =
            when (a) {
                is PropertyDescriptor ->
                    if (a.isExtensionProperty())
                        DeclarationCategory.EXTENSION_PROPERTY
                    else
                        DeclarationCategory.TYPE_OR_VALUE
                is ConstructorDescriptor,
                is SimpleFunctionDescriptor ->
                    DeclarationCategory.FUNCTION
                is ClassifierDescriptor ->
                    DeclarationCategory.TYPE_OR_VALUE
                else ->
                    error("Unexpected declaration kind: $a")
            }

    @JvmStatic fun groupModulePackageMembersByFqName(
            c: BodiesResolveContext,
            overloadFilter: OverloadFilter
    ): MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot> {
        val packageMembersByName = MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>()

        collectModulePackageMembersWithSameName(packageMembersByName, c.functions.values + c.declaredClasses.values, overloadFilter) {
            scope, name ->
            val functions = scope.getContributedFunctions(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            if (classifier is ClassDescriptor && !classifier.kind.isSingleton)
                functions + classifier.constructors
            else
                functions
        }

        collectModulePackageMembersWithSameName(packageMembersByName, c.properties.values, overloadFilter) {
            scope, name ->
            val variables = scope.getContributedVariables(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            variables + classifier.singletonOrEmptyList()
        }

        return packageMembersByName
    }

    private inline fun collectModulePackageMembersWithSameName(
            packageMembersByName: MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>,
            interestingDescriptors: Collection<DeclarationDescriptor>,
            overloadFilter: OverloadFilter,
            getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ) {
        val observedFQNs = hashSetOf<FqNameUnsafe>()
        for (descriptor in interestingDescriptors) {
            if (descriptor.containingDeclaration !is PackageFragmentDescriptor) continue

            val descriptorFQN = DescriptorUtils.getFqName(descriptor)
            if (observedFQNs.contains(descriptorFQN)) continue
            observedFQNs.add(descriptorFQN)

            val packageMembersWithSameName = getModulePackageMembersWithSameName(descriptor, overloadFilter, getMembersByName)
            packageMembersByName.putValues(descriptorFQN, packageMembersWithSameName)
        }
    }

    private inline fun getModulePackageMembersWithSameName(
            descriptor: DeclarationDescriptor,
            overloadFilter: OverloadFilter,
            getMembersByName: (MemberScope, Name) -> Collection<DeclarationDescriptorNonRoot>
    ): Collection<DeclarationDescriptorNonRoot> {
        val containingPackage = descriptor.containingDeclaration
        if (containingPackage !is PackageFragmentDescriptor) {
            throw AssertionError("$descriptor is not a top-level package member")
        }

        val containingModule = DescriptorUtils.getContainingModuleOrNull(descriptor) ?:
                               return when (descriptor) {
                                   is CallableMemberDescriptor -> listOf(descriptor)
                                   is ClassDescriptor -> descriptor.constructors
                                   else -> throw AssertionError("Unexpected descriptor kind: $descriptor")
                               }

        val containingPackageScope = containingModule.getPackage(containingPackage.fqName).memberScope
        val possibleOverloads =
                getMembersByName(containingPackageScope, descriptor.name).filter {
                    // NB memberScope for PackageViewDescriptor includes module dependencies
                    DescriptorUtils.getContainingModule(it) == containingModule
                }

        return overloadFilter.filterPackageMemberOverloads(possibleOverloads)
    }

    private fun DeclarationDescriptor.isPrivate() =
            this is DeclarationDescriptorWithVisibility &&
            Visibilities.isPrivate(this.visibility)

    @JvmStatic fun getPossibleRedeclarationGroups(
            members: Collection<DeclarationDescriptorNonRoot>
    ): Collection<Collection<DeclarationDescriptorNonRoot>> {
        val result = arrayListOf<Collection<DeclarationDescriptorNonRoot>>()

        val nonPrivates = members.filter { !it.isPrivate() }
        if (nonPrivates.size > 1) {
            result.add(nonPrivates)
        }

        val bySourceFile = MultiMap.createSmart<SourceFile, DeclarationDescriptorNonRoot>()
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
