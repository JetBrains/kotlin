/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportOnDeclaration
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.getTypeAliasConstructors
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class OverloadResolver(
        private val trace: BindingTrace,
        private val overloadFilter: OverloadFilter,
        private val overloadChecker: OverloadChecker
) {

    fun checkOverloads(c: BodiesResolveContext) {
        val inClasses = findConstructorsInNestedClassesAndTypeAliases(c)

        for ((key, value) in c.declaredClasses) {
            checkOverloadsInClass(value, inClasses.get(value))
        }
        checkOverloadsInPackages(c)
    }

    private fun findConstructorsInNestedClassesAndTypeAliases(c: BodiesResolveContext): MultiMap<ClassDescriptor, FunctionDescriptor> {
        val constructorsByOuterClass = MultiMap.create<ClassDescriptor, FunctionDescriptor>()

        for (klass in c.declaredClasses.values) {
            if (klass.kind.isSingleton || klass.name.isSpecial) {
                // Constructors of singletons or anonymous object aren't callable from the code, so they shouldn't participate in overload name checking
                continue
            }
            val containingDeclaration = klass.containingDeclaration
            if (containingDeclaration is ScriptDescriptor) {
                // TODO: check overload conflicts of functions with constructors in scripts
            }
            else if (containingDeclaration is ClassDescriptor) {
                constructorsByOuterClass.putValues(containingDeclaration, klass.constructors)
            }
            else if (!(containingDeclaration is FunctionDescriptor ||
                       containingDeclaration is PropertyDescriptor ||
                       containingDeclaration is PackageFragmentDescriptor)) {
                throw IllegalStateException("Illegal class container: " + containingDeclaration)
            }
        }

        for (typeAlias in c.typeAliases.values) {
            val containingDeclaration = typeAlias.containingDeclaration
            if (containingDeclaration is ClassDescriptor) {
                constructorsByOuterClass.putValues(containingDeclaration, typeAlias.getTypeAliasConstructors())
            }
        }

        return constructorsByOuterClass
    }

    private fun checkOverloadsInPackages(c: BodiesResolveContext) {
        val membersByName = groupModulePackageMembersByFqName(c, overloadFilter)

        for (e in membersByName.entrySet()) {
            checkOverloadsInPackage(e.value)
        }
    }

    private fun groupModulePackageMembersByFqName(
            c: BodiesResolveContext,
            overloadFilter: OverloadFilter
    ): MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot> {
        val packageMembersByName = MultiMap<FqNameUnsafe, DeclarationDescriptorNonRoot>()

        collectModulePackageMembersWithSameName(
                packageMembersByName,
                c.functions.values + c.declaredClasses.values + c.typeAliases.values,
                overloadFilter
        ) {
            scope, name ->
            val functions = scope.getContributedFunctions(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            val classifier = scope.getContributedClassifier(name, NoLookupLocation.WHEN_CHECK_REDECLARATIONS)
            when (classifier) {
                is ClassDescriptor ->
                    if (!classifier.kind.isSingleton)
                        functions + classifier.constructors
                    else
                        functions
                is TypeAliasDescriptor ->
                    functions + classifier.getTypeAliasConstructors()
                else ->
                    functions
            }
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

    private fun checkOverloadsInClass(
            classDescriptor: ClassDescriptorWithResolutionScopes,
            nestedClassConstructors: Collection<FunctionDescriptor>
    ) {
        val functionsByName = MultiMap.create<Name, CallableMemberDescriptor>()

        for (function in classDescriptor.declaredCallableMembers) {
            functionsByName.putValue(function.name, function)
        }

        for (nestedConstructor in nestedClassConstructors) {
            val name = nestedConstructor.containingDeclaration.name
            functionsByName.putValue(name, nestedConstructor)
        }

        for (e in functionsByName.entrySet()) {
            checkOverloadsInClass(e.value)
        }
    }

    private fun checkOverloadsInPackage(members: Collection<DeclarationDescriptorNonRoot>) {
        if (members.size == 1) return

        val redeclarationsMap = LinkedHashMap<DeclarationDescriptorNonRoot, MutableSet<DeclarationDescriptorNonRoot>>()
        for (redeclarationGroup in getPossibleRedeclarationGroups(members)) {
            val redeclarations = findRedeclarations(redeclarationGroup)
            redeclarations.forEach {
                redeclarationsMap.getOrPut(it) { LinkedHashSet() }.addAll(redeclarations)
            }
        }

        val reported = HashSet<DeclarationDescriptorNonRoot>()
        for ((member, conflicting) in redeclarationsMap) {
            if (!reported.contains(member)) {
                reported.addAll(conflicting)
                reportRedeclarations(conflicting)
            }
        }
    }

    private fun getPossibleRedeclarationGroups(members: Collection<DeclarationDescriptorNonRoot>): Collection<Collection<DeclarationDescriptorNonRoot>> {
        val result = arrayListOf<Collection<DeclarationDescriptorNonRoot>>()

        val nonPrivates = members.filter { !it.isPrivate() }

        val bySourceFile = members.groupBy { DescriptorUtils.getContainingSourceFile(it) }

        var hasGroupIncludingNonPrivateMembers = false
        for ((sourceFile, membersInFile) in bySourceFile) {
            // File member groups are interesting in redeclaration check if at least one file member is private.
            if (membersInFile.any { it.isPrivate() }) {
                hasGroupIncludingNonPrivateMembers = true
                val group = LinkedHashSet<DeclarationDescriptorNonRoot>(nonPrivates) + membersInFile
                result.add(group)
            }
        }

        if (!hasGroupIncludingNonPrivateMembers && nonPrivates.size > 1) {
            result.add(nonPrivates)
        }

        return result
    }

    private fun DeclarationDescriptor.isPrivate() =
            this is DeclarationDescriptorWithVisibility &&
            Visibilities.isPrivate(this.visibility)

    private fun checkOverloadsInClass(members: Collection<CallableMemberDescriptor>) {
        if (members.size == 1) return
        reportRedeclarations(findRedeclarations(members))
    }

    private fun DeclarationDescriptor.isSynthesized() =
            this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    private fun findRedeclarations(members: Collection<DeclarationDescriptorNonRoot>): Collection<DeclarationDescriptorNonRoot> {
        val redeclarations = linkedSetOf<DeclarationDescriptorNonRoot>()
        for (member1 in members) {
            if (member1.isSynthesized()) continue

            for (member2 in members) {
                if (member1 == member2) continue
                if (isConstructorsOfDifferentRedeclaredClasses(member1, member2)) continue
                if (isTopLevelMainInDifferentFiles(member1, member2)) continue

                if (!overloadChecker.isOverloadable(member1, member2)) {
                    redeclarations.add(member1)
                }
            }
        }
        return redeclarations
    }

    private fun isConstructorsOfDifferentRedeclaredClasses(member1: DeclarationDescriptor, member2: DeclarationDescriptor): Boolean {
        if (member1 !is ConstructorDescriptor || member2 !is ConstructorDescriptor) return false
        // ignore conflicting overloads for constructors of different classes because their redeclarations will be reported
        // but don't ignore if there's possibility that classes redeclarations will not be reported
        // (e.g. they're declared in different packages)
        val parent1 = member1.containingDeclaration
        val parent2 = member2.containingDeclaration
        return parent1 !== parent2 && parent1.containingDeclaration == parent2.containingDeclaration
    }

    private fun isTopLevelMainInDifferentFiles(member1: DeclarationDescriptor, member2: DeclarationDescriptor): Boolean {
        if (!MainFunctionDetector.isMain(member1) || !MainFunctionDetector.isMain(member2)) {
            return false
        }

        val file1 = DescriptorToSourceUtils.getContainingFile(member1)
        val file2 = DescriptorToSourceUtils.getContainingFile(member2)
        return file1 == null || file2 == null || file1 !== file2
    }

    private fun reportRedeclarations(redeclarations: Collection<DeclarationDescriptorNonRoot>) {
        if (redeclarations.isEmpty()) return

        for (memberDescriptor in redeclarations) {
            when (memberDescriptor) {
                is PropertyDescriptor,
                is ClassifierDescriptor ->
                    reportOnDeclaration(trace, memberDescriptor) { Errors.REDECLARATION.on(it, redeclarations) }
                is FunctionDescriptor ->
                    reportOnDeclaration(trace, memberDescriptor) { Errors.CONFLICTING_OVERLOADS.on(it, redeclarations) }
            }
        }
    }
}
