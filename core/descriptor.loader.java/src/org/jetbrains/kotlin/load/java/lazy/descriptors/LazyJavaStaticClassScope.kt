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

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValueOfMethod
import org.jetbrains.kotlin.resolve.DescriptorFactory.createEnumValuesMethod
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LookupLocation
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
        result.addIfNotNull(c.samConversionResolver.resolveSamConstructor(name, nestedClassesScope))

        if (jClass.isEnum()) {
            when (name) {
                DescriptorUtils.ENUM_VALUE_OF -> result.add(createEnumValueOfMethod(getContainingDeclaration()))
                DescriptorUtils.ENUM_VALUES -> result.add(createEnumValuesMethod(getContainingDeclaration()))
            }
        }
    }

    override fun getContainingDeclaration() = super.getContainingDeclaration() as LazyJavaClassDescriptor
}
