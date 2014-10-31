/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.name.*
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.lazy.withTypes
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.lazy.findClassInJava
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import org.jetbrains.jet.lang.resolve.DescriptorFactory.*
import org.jetbrains.jet.utils.addIfNotNull

public abstract class LazyJavaStaticScope(
        c: LazyJavaResolverContext,
        descriptor: ClassOrPackageFragmentDescriptor
) : LazyJavaMemberScope(c.withTypes(), descriptor) {

    override fun getDispatchReceiverParameter() = null

    // Package fragments are not nested
    override fun getPackage(name: Name) = null
    abstract fun getSubPackages(): Collection<FqName>

    override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = listOf()

    override fun resolveMethodSignature(
            method: JavaMethod, methodTypeParameters: List<TypeParameterDescriptor>, returnType: JetType,
            valueParameters: LazyJavaMemberScope.ResolvedValueParameters
    ): LazyJavaMemberScope.MethodSignatureData {
        val effectiveSignature = c.externalSignatureResolver.resolveAlternativeMethodSignature(
                method, false, returnType, null, valueParameters.descriptors, methodTypeParameters, false)
        return LazyJavaMemberScope.MethodSignatureData(effectiveSignature, listOf(), effectiveSignature.getErrors())
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        //no undeclared properties
    }
}

public class LazyPackageFragmentScopeForJavaPackage(
        c: LazyJavaResolverContext,
        private val jPackage: JavaPackage,
        packageFragment: LazyJavaPackageFragment
) : LazyJavaStaticScope(c, packageFragment) {

    // TODO: Storing references is a temporary hack until modules infrastructure is implemented.
    // See JetTypeMapperWithOutDirectories for details
    public val kotlinBinaryClass: KotlinJvmBinaryClass?
            = c.kotlinClassFinder.findKotlinClass(PackageClassUtils.getPackageClassId(packageFragment.fqName))

    private val deserializedPackageScope = c.storageManager.createLazyValue {
        val kotlinBinaryClass = kotlinBinaryClass
        if (kotlinBinaryClass == null)
            JetScope.Empty
        else
            c.deserializedDescriptorResolver.createKotlinPackageScope(packageFragment, kotlinBinaryClass) ?: JetScope.Empty
    }

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { name ->
        val classId = ClassId(packageFragment.fqName, SpecialNames.safeIdentifier(name))
        val (jClass, kClass) = this.c.findClassInJava(classId)
        if (kClass != null)
            kClass
        else if (jClass == null)
            null
        else {
            val classDescriptor = this.c.javaClassResolver.resolveClass(jClass)
            assert(classDescriptor == null || classDescriptor.getContainingDeclaration() == packageFragment) {
                "Wrong package fragment for $classDescriptor, expected $packageFragment"
            }
            classDescriptor
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = classes(name)

    override fun getProperties(name: Name) = deserializedPackageScope().getProperties(name)
    override fun getFunctions(name: Name) = deserializedPackageScope().getFunctions(name) + super.getFunctions(name)

    override fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>,
                                     kindFilter: (JetScope.DescriptorKind) -> Boolean,
                                     nameFilter: (Name) -> Boolean) {
        result.addAll(deserializedPackageScope().getDescriptors(kindFilter, nameFilter))
    }

    override fun computeMemberIndex(): MemberIndex = object : MemberIndex by EMPTY_MEMBER_INDEX {
        // For SAM-constructors
        override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name> = getClassNames(nameFilter)
    }

    override fun getClassNames(nameFilter: (Name) -> Boolean): Collection<Name> {
        return jPackage.getClasses(nameFilter).stream()
                .filter { c -> c.getOriginKind() != JavaClass.OriginKind.KOTLIN_LIGHT_CLASS }
                .map { c -> c.getName() }.toList()
    }

    private val subPackages = c.storageManager.createRecursionTolerantLazyValue(
            {
                jPackage.getSubPackages().map { sp -> sp.getFqName() }
            },
            // This breaks infinite recursion between loading Java descriptors and building light classes
            onRecursiveCall = listOf()
    )

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        result.addIfNotNull(c.samConversionResolver.resolveSamConstructor(name, this))
    }

    override fun getSubPackages() = subPackages()

    override fun getAllPropertyNames() = listOf<Name>()

    // we don't use implementation from super which caches all descriptors and does not use filters
    override fun getDescriptors(kindFilter: (JetScope.DescriptorKind) -> Boolean, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptors(kindFilter, nameFilter)
    }
}

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

    override fun getFunctionNames(nameFilter: (Name) -> Boolean): Collection<Name> {
        if (jClass.isEnum()) {
            return super.getFunctionNames(nameFilter) + listOf(DescriptorUtils.ENUM_VALUE_OF, DescriptorUtils.ENUM_VALUES)
        }
        return super.getFunctionNames(nameFilter)
    }

    override fun getClassNames(nameFilter: (Name) -> Boolean): Collection<Name> = listOf()
    override fun getClassifier(name: Name): ClassifierDescriptor? = null

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
