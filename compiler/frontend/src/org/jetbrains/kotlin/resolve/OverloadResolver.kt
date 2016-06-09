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
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.calls.tower.getTypeAliasConstructors
import org.jetbrains.kotlin.utils.addToStdlib.check

class OverloadResolver(
        private val trace: BindingTrace,
        private val overloadFilter: OverloadFilter,
        private val overloadChecker: OverloadChecker
) {

    fun checkOverloads(c: BodiesResolveContext) {
        val inClasses = findConstructorsInNestedClassesAndTypeAliases(c)

        for (entry in c.declaredClasses.entries) {
            checkOverloadsInClass(entry.value, inClasses.get(entry.value))
        }
        checkOverloadsInPackages(c)
    }

    private fun findConstructorsInNestedClassesAndTypeAliases(c: BodiesResolveContext): MultiMap<ClassDescriptor, ConstructorDescriptor> {
        val constructorsByOuterClass = MultiMap.create<ClassDescriptor, ConstructorDescriptor>()

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
        val membersByName = overloadChecker.groupModulePackageMembersByFqName(c, overloadFilter)

        for (e in membersByName.entrySet()) {
            checkOverloadsInPackage(e.value)
        }
    }

    private fun checkOverloadsInClass(
            classDescriptor: ClassDescriptorWithResolutionScopes,
            nestedClassConstructors: Collection<ConstructorDescriptor>
    ) {
        val functionsByName = MultiMap.create<Name, CallableMemberDescriptor>()

        for (function in classDescriptor.declaredCallableMembers) {
            functionsByName.putValue(function.name, function)
        }

        for (nestedConstructor in nestedClassConstructors) {
            val name =
                    if (nestedConstructor is TypeAliasConstructorDescriptor)
                        nestedConstructor.typeAliasDescriptor.name
                    else
                        nestedConstructor.containingDeclaration.name

            functionsByName.putValue(name, nestedConstructor)
        }

        for (e in functionsByName.entrySet()) {
            checkOverloadsInClass(e.value)
        }
    }

    private fun checkOverloadsInPackage(members: Collection<DeclarationDescriptorNonRoot>) {
        if (members.size == 1) return
        for (redeclarationGroup in overloadChecker.getPossibleRedeclarationGroups(members)) {
            reportRedeclarations(findRedeclarations(redeclarationGroup))
        }
    }

    private fun checkOverloadsInClass(members: Collection<CallableMemberDescriptor>) {
        if (members.size == 1) return
        reportRedeclarations(findRedeclarations(members))
    }

    private fun DeclarationDescriptor.isSynthesized() =
            this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.SYNTHESIZED

    private fun findRedeclarations(members: Collection<DeclarationDescriptorNonRoot>): Set<Pair<KtDeclaration?, DeclarationDescriptorNonRoot>> {
        val redeclarations = linkedSetOf<Pair<KtDeclaration?, DeclarationDescriptorNonRoot>>()
        for (member1 in members) {
            if (member1.isSynthesized()) continue

            for (member2 in members) {
                if (member1 == member2) continue
                if (isConstructorsOfDifferentRedeclaredClasses(member1, member2)) continue
                if (isTopLevelMainInDifferentFiles(member1, member2)) continue

                if (!overloadChecker.isOverloadable(member1, member2)) {
                    val ktDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(member1) as KtDeclaration?
                    redeclarations.add(ktDeclaration to member1)
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

    private fun reportRedeclarations(redeclarations: Set<Pair<KtDeclaration?, DeclarationDescriptorNonRoot>>) {
        if (redeclarations.isEmpty()) return

        val redeclarationsIterator = redeclarations.iterator()
        val firstRedeclarationDescriptor = redeclarationsIterator.next().second
        val otherRedeclarationDescriptor = redeclarationsIterator.check { it.hasNext() }?.next()?.second

        for ((ktDeclaration, memberDescriptor) in redeclarations) {
            if (ktDeclaration == null) continue

            when (memberDescriptor) {
                is PropertyDescriptor,
                is ClassifierDescriptor -> {
                    trace.report(Errors.REDECLARATION.on(ktDeclaration, memberDescriptor.name.asString()))
                }
                is FunctionDescriptor -> {
                    val redeclarationDescriptor =
                            if (otherRedeclarationDescriptor == null)
                                firstRedeclarationDescriptor
                            else if (memberDescriptor == firstRedeclarationDescriptor)
                                otherRedeclarationDescriptor
                            else
                                firstRedeclarationDescriptor

                    trace.report(Errors.CONFLICTING_OVERLOADS.on(ktDeclaration, memberDescriptor,
                                                                 redeclarationDescriptor.containingDeclaration))
                }
            }
        }
    }
}
