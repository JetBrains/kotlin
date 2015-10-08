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

package org.jetbrains.kotlin.load.java.lazy.descriptors

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.DescriptorResolverUtils
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValueOfMethod
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValuesMethod
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.addIfNotNull

public class LazyJavaStaticClassScope(
        c: LazyJavaResolverContext,
        private val jClass: JavaClass,
        descriptor: LazyJavaClassDescriptor
) : LazyJavaStaticScope(c, descriptor) {

    override fun computeMemberIndex(): MemberIndex {
        val delegate = ClassMemberIndex(jClass) { it.isStatic() }
        return object : MemberIndex by delegate {
            override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name> {
                // Should be a super call, but KT-2860
                return delegate.getMethodNames(nameFilter) +
                       // For SAM-constructors
                       jClass.getInnerClasses().map { it.getName() }
            }
        }
    }

    override fun getFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> {
        if (jClass.isEnum()) {
            return super.getFunctionNames(kindFilter, nameFilter) + listOf(DescriptorUtils.ENUM_VALUE_OF, DescriptorUtils.ENUM_VALUES)
        }
        return super.getFunctionNames(kindFilter, nameFilter)
    }

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> =
            memberIndex().getAllFieldNames()

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> = listOf()
    override fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = null

    override fun getSubPackages(): Collection<FqName> = listOf()

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val nestedClassesScope = getContainingDeclaration().getUnsubstitutedInnerClassesScope()
        result.addIfNotNull(c.components.samConversionResolver.resolveSamConstructor(name, nestedClassesScope))

        val functionsFromSupertypes = getStaticFunctionsFromJavaSuperClasses(name, getContainingDeclaration())
        result.addAll(DescriptorResolverUtils.resolveOverrides(name, functionsFromSupertypes, result, getContainingDeclaration(), c.components.errorReporter))

        if (jClass.isEnum()) {
            when (name) {
                DescriptorUtils.ENUM_VALUE_OF -> result.add(createEnumValueOfMethod(getContainingDeclaration()))
                DescriptorUtils.ENUM_VALUES -> result.add(createEnumValuesMethod(getContainingDeclaration()))
            }
        }
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        val propertiesFromSupertypes = getStaticPropertiesFromJavaSupertypes(name, getContainingDeclaration())

        val actualProperties =
                if (!result.isEmpty()) {
                    DescriptorResolverUtils.resolveOverrides(name, propertiesFromSupertypes, result, getContainingDeclaration(), c.components.errorReporter)
                }
                else {
                    propertiesFromSupertypes.groupBy {
                        it.realOriginal
                    }.flatMap {
                        DescriptorResolverUtils.resolveOverrides(name, it.value, result, getContainingDeclaration(), c.components.errorReporter)
                    }
                }

        result.addAll(actualProperties)
    }

    override fun getContainingDeclaration() = super.getContainingDeclaration() as LazyJavaClassDescriptor

    private fun getStaticFunctionsFromJavaSuperClasses(name: Name, descriptor: ClassDescriptor): Set<SimpleFunctionDescriptor> {
        val superClassDescriptor = descriptor.getSuperClassNotAny() ?: return emptySet()

        val staticScope = superClassDescriptor.staticScope

        if (staticScope !is LazyJavaStaticClassScope) return getStaticFunctionsFromJavaSuperClasses(name, superClassDescriptor)

        return staticScope.getFunctions(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { it as SimpleFunctionDescriptor }.toSet()
    }

    private fun getStaticPropertiesFromJavaSupertypes(name: Name, descriptor: ClassDescriptor): Set<PropertyDescriptor> {

        fun getStaticProperties(supertype: JetType): Iterable<PropertyDescriptor> {
            val superTypeDescriptor = supertype.constructor.declarationDescriptor as? ClassDescriptor ?: return emptyList()

            val staticScope = superTypeDescriptor.staticScope

            if (staticScope !is LazyJavaStaticClassScope) return getStaticPropertiesFromJavaSupertypes(name, superTypeDescriptor)

            return staticScope.getProperties(name, NoLookupLocation.WHEN_GET_SUPER_MEMBERS).map { it as PropertyDescriptor }
        }

        return descriptor.typeConstructor.supertypes.flatMap(::getStaticProperties).toSet()
    }

    private val PropertyDescriptor.realOriginal: PropertyDescriptor
        get() {
            if (this.kind.isReal) return this

            return this.overriddenDescriptors.map { it.realOriginal }.distinct().single()
        }
}
