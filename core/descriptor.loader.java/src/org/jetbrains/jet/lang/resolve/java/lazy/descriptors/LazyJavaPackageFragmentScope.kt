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
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.Collections
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaResolverContext
import org.jetbrains.jet.lang.resolve.java.lazy.withTypes
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.utils.flatten
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.kotlin.util.inn
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.java.lazy.findJavaClass
import org.jetbrains.jet.lang.resolve.java.lazy.findClassInJava
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaMemberScope.MethodSignatureData
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor
import org.jetbrains.jet.lang.resolve.name.SpecialNames
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass

public abstract class LazyJavaPackageFragmentScope(
        c: LazyJavaResolverContext,
        packageFragment: LazyJavaPackageFragment
) : LazyJavaMemberScope(c.withTypes(), packageFragment) {
    
    protected val fqName: FqName = DescriptorUtils.getFqName(packageFragment).toSafe()

    protected fun computeMemberIndexForSamConstructors(delegate: MemberIndex): MemberIndex = object : MemberIndex by delegate {
        override fun getAllMethodNames(): Collection<Name> {
            val jClass = c.findJavaClass(fqName)
            return delegate.getAllMethodNames() +
                   // For SAM-constructors
                   getAllClassNames() +
                   jClass.inn({ jC -> jC.getInnerClasses().map { c -> c.getName() }}, listOf())
        }
    }

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

    override fun getContainingDeclaration() = super.getContainingDeclaration() as LazyJavaPackageFragment

    fun ClassifierDescriptor.createSamConstructor(): SamConstructorDescriptor? {
        if (this is LazyJavaClassDescriptor && this.getFunctionTypeForSamInterface() != null) {
            return SingleAbstractMethodUtils.createSamConstructorFunction(this@LazyJavaPackageFragmentScope.getContainingDeclaration(), this)
        }
        return null
    }
}

public class LazyPackageFragmentScopeForJavaPackage(
        c: LazyJavaResolverContext,
        private val jPackage: JavaPackage,
        packageFragment: LazyPackageFragmentForJavaPackage
) : LazyJavaPackageFragmentScope(c, packageFragment) {

    // TODO: Storing references is a temporary hack until modules infrastructure is implemented.
    // See JetTypeMapperWithOutDirectories for details
    public val kotlinBinaryClass: KotlinJvmBinaryClass?
            = c.kotlinClassFinder.findKotlinClass(PackageClassUtils.getPackageClassFqName(fqName))

    private val deserializedPackageScope = c.storageManager.createLazyValue {
        val kotlinBinaryClass = kotlinBinaryClass
        if (kotlinBinaryClass == null)
            JetScope.EMPTY
        else
            c.deserializedDescriptorResolver.createKotlinPackageScope(packageFragment, kotlinBinaryClass) ?: JetScope.EMPTY
    }

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
        name ->
        val fqName = fqName.child(SpecialNames.safeIdentifier(name))
        val (jClass, kClass) = c.findClassInJava(fqName)
        if (kClass != null)
            kClass
        else if (jClass == null)
            null
        else {
            // TODO: this caching is a temporary workaround, should be replaced with properly caching the whole LazyJavaSubModule
            val cached = c.javaResolverCache.getClass(jClass)
            if (cached != null)
                cached
            else {
                val classDescriptor = c.javaClassResolver.resolveClass(jClass)
                assert(classDescriptor == null || classDescriptor.getContainingDeclaration() == packageFragment,
                       "Wrong package fragment for $classDescriptor, expected $packageFragment")
                classDescriptor
            }
        }
    }

    override fun getClassifier(name: Name): ClassifierDescriptor? = classes(name)

    override fun getProperties(name: Name) = deserializedPackageScope().getProperties(name)
    override fun getFunctions(name: Name) = deserializedPackageScope().getFunctions(name) + super.getFunctions(name)

    override fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>) {
        result.addAll(deserializedPackageScope().getAllDescriptors())
    }

    override fun computeMemberIndex(): MemberIndex = computeMemberIndexForSamConstructors(EMPTY_MEMBER_INDEX)

    override fun getAllClassNames(): Collection<Name> {
        return jPackage.getClasses().stream()
                .filter { c -> c.getOriginKind() != JavaClass.OriginKind.KOTLIN_LIGHT_CLASS }
                .map { c -> c.getName() }.toList()
    }

    private val _subPackages = c.storageManager.createRecursionTolerantLazyValue(
                                {
                                    listOf(
                                        // We do not filter by hasStaticMembers() because it's slow (e.g. it triggers light class generation),
                                        // and there's no harm in having some names in the result that can not be resolved
                                        jPackage.getClasses().map { c -> c.getFqName().sure("Toplevel class has no fqName: $c}") },
                                        jPackage.getSubPackages().map { sp -> sp.getFqName() }
                                    ).flatten()
                                },
                                // This breaks infinite recursion between loading Java descriptors and building light classes
                                onRecursiveCall = listOf())

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        val samConstructor = getClassifier(name)?.createSamConstructor()
        if (samConstructor != null) {
            result.add(samConstructor)
        }
    }

    override fun getSubPackages() = _subPackages()

    override fun getAllPropertyNames() = Collections.emptyList<Name>()
}

public class LazyPackageFragmentScopeForJavaClass(
        c: LazyJavaResolverContext,
        private val jClass: JavaClass,
        packageFragment: LazyPackageFragmentForJavaClass
) : LazyJavaPackageFragmentScope(c, packageFragment) {

    override fun computeMemberIndex(): MemberIndex = computeMemberIndexForSamConstructors(ClassMemberIndex(jClass, { m -> m.isStatic() }))

    // nested classes are loaded as members of their outer classes, not packages
    override fun getAllClassNames(): Collection<Name> = listOf()
    override fun getClassifier(name: Name): ClassifierDescriptor? = null

    // We do not filter by hasStaticMembers() because it's slow (e.g. it triggers light class generation),
    // and there's no harm in having some names in the result that can not be resolved
    override fun getSubPackages(): Collection<FqName> = jClass.getInnerClasses().stream()
                                                                .filter { c -> c.isStatic() }
                                                                .map { c -> c.getFqName().sure("Nested class has no fqName: $c}") }.toList()

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        //NOTE: assuming that all sam constructors are created for interfaces which are static and should be placed in this scope
        val samConstructor = getContainingDeclaration().getCorrespondingClass().getUnsubstitutedInnerClassesScope().getClassifier(name)
                ?.createSamConstructor()
        if (samConstructor != null) {
            result.add(samConstructor)
        }
    }

    override fun getContainingDeclaration() = super.getContainingDeclaration() as LazyPackageFragmentForJavaClass
}
