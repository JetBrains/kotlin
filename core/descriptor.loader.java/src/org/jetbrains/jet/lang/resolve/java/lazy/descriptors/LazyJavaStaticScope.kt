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
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaMemberScope.MethodSignatureData
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass
import org.jetbrains.jet.lang.resolve.DescriptorFactory.*

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
        return MethodSignatureData(effectiveSignature, listOf(), effectiveSignature.getErrors())
    }

    override fun computeNonDeclaredProperties(name: Name, result: MutableCollection<PropertyDescriptor>) {
        //no undeclared properties
    }

    override fun getContainingDeclaration() = super.getContainingDeclaration() as ClassOrPackageFragmentDescriptor

    fun ClassifierDescriptor.createSamConstructor(): SamConstructorDescriptor? {
        if (this is LazyJavaClassDescriptor && this.getFunctionTypeForSamInterface() != null) {
            return SingleAbstractMethodUtils.createSamConstructorFunction(this@LazyJavaStaticScope.getContainingDeclaration(), this)
        }
        return null
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
            JetScope.EMPTY
        else
            c.deserializedDescriptorResolver.createKotlinPackageScope(packageFragment, kotlinBinaryClass) ?: JetScope.EMPTY
    }

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
        name ->
        val classId = ClassId(packageFragment.fqName, SpecialNames.safeIdentifier(name))
        val (jClass, kClass) = c.findClassInJava(classId)
        if (kClass != null)
            kClass
        else if (jClass == null)
            null
        else {
            val classDescriptor = c.javaClassResolver.resolveClass(jClass)
            assert(classDescriptor == null || classDescriptor.getContainingDeclaration() == packageFragment,
                   "Wrong package fragment for $classDescriptor, expected $packageFragment")
            classDescriptor
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = classes(name)

    override fun getProperties(name: Name) = deserializedPackageScope().getProperties(name)
    override fun getFunctions(name: Name) = deserializedPackageScope().getFunctions(name) + super.getFunctions(name)

    override fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>) {
        result.addAll(deserializedPackageScope().getAllDescriptors())
    }

    override fun computeMemberIndex(): MemberIndex = object : MemberIndex by EMPTY_MEMBER_INDEX {
        // For SAM-constructors
        override fun getAllMethodNames(): Collection<Name> = getAllClassNames()
    }

    override fun computeAdditionalFunctions(name: Name) = listOf<SimpleFunctionDescriptor>()

    override fun getAllClassNames(): Collection<Name> {
        return jPackage.getClasses().stream()
                .filter { c -> c.getOriginKind() != JavaClass.OriginKind.KOTLIN_LIGHT_CLASS }
                .map { c -> c.getName() }.toList()
    }

    private val _subPackages = c.storageManager.createRecursionTolerantLazyValue(
            {
                jPackage.getSubPackages().map { sp -> sp.getFqName() }
            },
            // This breaks infinite recursion between loading Java descriptors and building light classes
            onRecursiveCall = listOf()
    )

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val samConstructor = getClassifier(name)?.createSamConstructor()
        if (samConstructor != null) {
            result.add(samConstructor)
        }
    }

    override fun getSubPackages() = _subPackages()

    override fun getAllPropertyNames() = Collections.emptyList<Name>()
}

public class LazyJavaStaticClassScope(
        c: LazyJavaResolverContext,
        private val jClass: JavaClass,
        descriptor: LazyJavaClassDescriptor
) : LazyJavaStaticScope(c, descriptor) {

    override fun computeMemberIndex(): MemberIndex {
        val delegate = ClassMemberIndex(jClass) { m -> m.isStatic() }
        return object : MemberIndex by delegate {
            override fun getAllMethodNames(): Collection<Name> {
                // Should be a super call, but KT-2860
                return delegate.getAllMethodNames() +
                       // For SAM-constructors
                       jClass.getInnerClasses().map { c -> c.getName() }
            }
        }
    }

    override fun getAllFunctionNames(): Collection<Name> {
        if (jClass.isEnum()) {
            return super.getAllFunctionNames() + listOf(DescriptorUtils.ENUM_VALUE_OF, DescriptorUtils.ENUM_VALUES)
        }
        return super.getAllFunctionNames()
    }

    override fun computeAdditionalFunctions(name: Name): Collection<SimpleFunctionDescriptor> {
        if (jClass.isEnum()) {
            when (name) {
                DescriptorUtils.ENUM_VALUE_OF -> return listOf(createEnumValueOfMethod(getContainingDeclaration()))
                DescriptorUtils.ENUM_VALUES -> return listOf(createEnumValuesMethod(getContainingDeclaration()))
            }
        }
        return listOf()
    }

    override fun getAllClassNames(): Collection<Name> = listOf()
    override fun getClassifier(name: Name): ClassifierDescriptor? = null

    override fun getSubPackages(): Collection<FqName> = listOf()

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        //NOTE: assuming that all sam constructors are created for interfaces which are static and should be placed in this scope
        val samConstructor = getContainingDeclaration().getUnsubstitutedInnerClassesScope().getClassifier(name)?.createSamConstructor()
        if (samConstructor != null) {
            result.add(samConstructor)
        }
    }

    override fun getContainingDeclaration() = super.getContainingDeclaration() as LazyJavaClassDescriptor
}
