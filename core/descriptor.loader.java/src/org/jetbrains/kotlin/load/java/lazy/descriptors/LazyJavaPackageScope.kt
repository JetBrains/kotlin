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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.load.java.lazy.KotlinClassLookupResult
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.resolveKotlinBinaryClass
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.KtScope
import org.jetbrains.kotlin.utils.addIfNotNull

public class LazyJavaPackageScope(
        c: LazyJavaResolverContext,
        private val jPackage: JavaPackage,
        private val containingDeclaration: LazyJavaPackageFragment
) : LazyJavaStaticScope(c, containingDeclaration) {
    private val partToFacade = c.storageManager.createLazyValue {
        val result = hashMapOf<String, String>()
        kotlinClasses@for (kotlinClass in containingDeclaration.kotlinBinaryClasses) {
            val header = kotlinClass.classHeader
            when (header.kind) {
                KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                    val partName = kotlinClass.classId.shortClassName.asString()
                    val facadeName = header.multifileClassName ?: continue@kotlinClasses
                    result[partName] = facadeName.substringAfterLast('/')
                }
                KotlinClassHeader.Kind.FILE_FACADE -> {
                    val fileFacadeName = kotlinClass.classId.shortClassName.asString()
                    result[fileFacadeName] = fileFacadeName
                }
                else -> {}
            }
        }
        result
    }

    public fun getFacadeSimpleNameForPartSimpleName(partName: String): String? =
            partToFacade()[partName]

    private val deserializedPackageScope = c.storageManager.createLazyValue {
        if (containingDeclaration.kotlinBinaryClasses.isEmpty()) {
            // If the scope is queried but no package parts are found, there's a possibility that we're trying to load symbols
            // from an old package with the binary-incompatible facade.
            // We try to read the old package facade if there is one, to report the "incompatible ABI version" message.
            packageFragment.oldPackageFacade?.let { binaryClass ->
                c.components.deserializedDescriptorResolver.readData(binaryClass, DeserializedDescriptorResolver.KOTLIN_PACKAGE_FACADE)
            }

            KtScope.Empty
        }
        else {
            c.components.deserializedDescriptorResolver.createKotlinPackageScope(packageFragment, containingDeclaration.kotlinBinaryClasses)
        }
    }

    private val packageFragment: LazyJavaPackageFragment get() = getContainingDeclaration() as LazyJavaPackageFragment

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { name ->
        val classId = ClassId(packageFragment.fqName, name)

        val kotlinResult = c.resolveKotlinBinaryClass(c.components.kotlinClassFinder.findKotlinClass(classId))
        when (kotlinResult) {
            is KotlinClassLookupResult.Found -> kotlinResult.descriptor
            is KotlinClassLookupResult.SyntheticClass -> null
            is KotlinClassLookupResult.NotFound -> {
                c.components.finder.findClass(classId)?.let { javaClass ->
                    c.javaClassResolver.resolveClass(javaClass).apply {
                        assert(this == null || this.containingDeclaration == packageFragment) {
                            "Wrong package fragment for $this, expected $packageFragment"
                        }
                    }
                }
            }
        }
    }

    override fun getClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? =
            if (SpecialNames.isSafeIdentifier(name)) classes(name) else null

    override fun getProperties(name: Name, location: LookupLocation) = deserializedPackageScope().getProperties(name, location)
    override fun getFunctions(name: Name, location: LookupLocation) = deserializedPackageScope().getFunctions(name, location) + super.getFunctions(name, location)

    override fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>,
                                     kindFilter: DescriptorKindFilter,
                                     nameFilter: (Name) -> Boolean) {
        result.addAll(deserializedPackageScope().getDescriptors(kindFilter, nameFilter))
    }

    override fun computeMemberIndex(): MemberIndex = object : MemberIndex by EMPTY_MEMBER_INDEX {
        // For SAM-constructors
        override fun getMethodNames(nameFilter: (Name) -> Boolean): Collection<Name> = getClassNames(DescriptorKindFilter.CLASSIFIERS, nameFilter)
    }

    override fun getClassNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> {
        // neither objects nor enum members can be in java package
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)) return listOf()

        return jPackage.getClasses(nameFilter).asSequence()
                .filter { c -> c.originKind != JavaClass.OriginKind.KOTLIN_LIGHT_CLASS }
                .map { c -> c.name }.toList()
    }

    override fun getFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<Name> {
        // optimization: only SAM-constructors may exist in java package
        if (kindFilter.excludes.contains(SamConstructorDescriptorKindExclude)) return listOf()

        return super.getFunctionNames(kindFilter, nameFilter)
    }

    private val subPackages = c.storageManager.createRecursionTolerantLazyValue(
            {
                jPackage.getSubPackages().map { sp -> sp.getFqName() }
            },
            // This breaks infinite recursion between loading Java descriptors and building light classes
            onRecursiveCall = listOf()
    )

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
        result.addIfNotNull(c.components.samConversionResolver.resolveSamConstructor(name, this))
    }

    override fun getSubPackages() = subPackages()

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = listOf<Name>()

    // we don't use implementation from super which caches all descriptors and does not use filters
    override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptors(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
    }
}
