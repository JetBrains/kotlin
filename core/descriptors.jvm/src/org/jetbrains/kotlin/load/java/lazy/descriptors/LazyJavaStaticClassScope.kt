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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils.resolveOverridesForStaticMembers
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getParentJavaStaticClassScope
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

class LazyJavaStaticClassScope(
    c: LazyJavaResolverContext,
    private val jClass: JavaClass,
    override val ownerDescriptor: JavaClassDescriptor
) : LazyJavaStaticScope(c) {

    override fun computeMemberIndex() = ClassDeclaredMemberIndex(jClass) { it.isStatic }

    override fun computeFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) =
        declaredMemberIndex().getMethodNames().toMutableSet().apply {
            addAll(ownerDescriptor.getParentJavaStaticClassScope()?.getFunctionNames().orEmpty())
            if (jClass.isEnum) {
                addAll(listOf(StandardNames.ENUM_VALUE_OF, StandardNames.ENUM_VALUES))
            }
            addAll(c.components.syntheticPartsProvider.getStaticFunctionNames(ownerDescriptor, c))
        }

    override fun computePropertyNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) =
        declaredMemberIndex().getFieldNames().toMutableSet().apply {
            flatMapJavaStaticSupertypesScopes(ownerDescriptor, this) { it.getVariableNames() }
            if (jClass.isEnum) {
                add(StandardNames.ENUM_ENTRIES)
            }
        }

    override fun computeClassNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> = emptySet()

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
        // We don't need to track lookups here because we find nested/inner classes in LazyJavaClassMemberScope
        return null
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val functionsFromSupertypes = getStaticFunctionsFromJavaSuperClasses(name, ownerDescriptor)
        result.addAll(
            resolveOverridesForStaticMembers(
                name,
                functionsFromSupertypes,
                result,
                ownerDescriptor,
                c.components.errorReporter,
                c.components.kotlinTypeChecker.overridingUtil
            )
        )

        if (jClass.isEnum) {
            when (name) {
                StandardNames.ENUM_VALUE_OF -> result.add(createEnumValueOfMethod(ownerDescriptor))
                StandardNames.ENUM_VALUES -> result.add(createEnumValuesMethod(ownerDescriptor))
            }
        }
    }

    override fun computeImplicitlyDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        c.components.syntheticPartsProvider.generateStaticFunctions(ownerDescriptor, name, result, c)
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val propertiesFromSupertypes = flatMapJavaStaticSupertypesScopes(ownerDescriptor, mutableSetOf()) {
            it.getContributedVariables(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS)
        }

        if (result.isNotEmpty()) {
            result.addAll(
                resolveOverridesForStaticMembers(
                    name,
                    propertiesFromSupertypes,
                    result,
                    ownerDescriptor,
                    c.components.errorReporter,
                    c.components.kotlinTypeChecker.overridingUtil
                )
            )
        } else {
            result.addAll(propertiesFromSupertypes.groupBy {
                it.realOriginal
            }.flatMap {
                resolveOverridesForStaticMembers(
                    name, it.value, result, ownerDescriptor, c.components.errorReporter,
                    c.components.kotlinTypeChecker.overridingUtil
                )
            })
        }
        if (jClass.isEnum) {
            when (name) {
                StandardNames.ENUM_ENTRIES ->
                    result.addIfNotNull(createEnumEntriesProperty(ownerDescriptor))
            }
        }
    }

    private fun getStaticFunctionsFromJavaSuperClasses(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
        val staticScope = descriptor.getParentJavaStaticClassScope() ?: return emptySet()
        return staticScope.getContributedFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).toSet()
    }

    private fun <R> flatMapJavaStaticSupertypesScopes(
        root: ClassDescriptor,
        result: MutableSet<R>,
        onJavaStaticScope: (MemberScope) -> Collection<R>
    ): Set<R> {
        DFS.dfs(listOf(root),
                {
                    it.typeConstructor.supertypes.asSequence().mapNotNull { supertype ->
                        supertype.constructor.declarationDescriptor as? ClassDescriptor
                    }.asIterable()
                },
                object : DFS.AbstractNodeHandler<ClassDescriptor, Unit>() {
                    override fun beforeChildren(current: ClassDescriptor): Boolean {
                        if (current === root) return true
                        val staticScope = current.staticScope

                        if (staticScope is LazyJavaStaticScope) {
                            result.addAll(onJavaStaticScope(staticScope))
                            return false
                        }
                        return true
                    }

                    override fun result() {}
                }
        )

        return result
    }

    private val PropertyDescriptor.realOriginal: PropertyDescriptor
        get() {
            if (this.kind.isReal) return this

            return this.overriddenDescriptors.map { it.realOriginal }.distinct().single()
        }
}
