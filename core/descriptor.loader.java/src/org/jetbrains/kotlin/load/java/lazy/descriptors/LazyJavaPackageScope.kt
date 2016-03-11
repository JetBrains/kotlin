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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.load.java.lazy.KotlinClassLookupResult
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.lazy.resolveKotlinBinaryClass
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.storage.getValue

class LazyJavaPackageScope(
        c: LazyJavaResolverContext,
        private val jPackage: JavaPackage,
        override val ownerDescriptor: LazyJavaPackageFragment
) : LazyJavaStaticScope(c) {

    // Null means that it's impossible to determine list of class names in package, i.e. in IDE where special finders exist
    // But for compiler though we can determine full list of class names by getting all class-file names in classpath and sources
    private val knownClassNamesInPackage: NullableLazyValue<Set<String>> = c.storageManager.createNullableLazyValue {
        c.components.finder.knownClassNamesInPackage(ownerDescriptor.fqName)
    }

    private val partToFacade = c.storageManager.createLazyValue {
        val result = hashMapOf<String, String>()
        kotlinClasses@for (kotlinClass in ownerDescriptor.kotlinBinaryClasses) {
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

    fun getFacadeSimpleNameForPartSimpleName(partName: String): String? =
            partToFacade()[partName]

    private val deserializedPackageScope by c.storageManager.createLazyValue {
        c.components.deserializedDescriptorResolver.createKotlinPackageScope(ownerDescriptor, ownerDescriptor.kotlinBinaryClasses)
    }

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<FindClassRequest, ClassDescriptor> { request ->
        val classId = ClassId(ownerDescriptor.fqName, request.name)

        val kotlinBinaryClass =
                // These branches should be semantically equal, but the first one could be faster
                if (request.javaClass != null)
                    c.components.kotlinClassFinder.findKotlinClass(request.javaClass)
                else
                    c.components.kotlinClassFinder.findKotlinClass(classId)

        val kotlinResult = c.resolveKotlinBinaryClass(kotlinBinaryClass)

        when (kotlinResult) {
            is KotlinClassLookupResult.Found -> kotlinResult.descriptor
            is KotlinClassLookupResult.SyntheticClass -> null
            is KotlinClassLookupResult.NotFound -> {

                val javaClass = request.javaClass ?: c.components.finder.findClass(classId)
                javaClass?.let { it ->
                    LazyJavaClassDescriptor(c, ownerDescriptor, it.fqName!!, it)
                }
            }
        }
    }

    // javaClass here is only for sake of optimizations
    private class FindClassRequest(val name: Name, val javaClass: JavaClass?) {
        override fun equals(other: Any?): Boolean{
            if (this === other) return true

            other as FindClassRequest

            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int{
            return name.hashCode()
        }
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation) = findClassifier(name, null, location)

    private fun findClassifier(name: Name, javaClass: JavaClass?, location: LookupLocation): ClassDescriptor? {
        if (!SpecialNames.isSafeIdentifier(name)) return null

        recordLookup(name, location)

        val knownClassNamesInPackage = knownClassNamesInPackage()
        if (javaClass == null && knownClassNamesInPackage != null && name.asString() !in knownClassNamesInPackage) {
            return null
        }

        return classes(FindClassRequest(name, javaClass))
    }

    fun findClassifierByJavaClass(javaClass: JavaClass, location: LookupLocation) = findClassifier(javaClass.name, javaClass, location)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
        // We should track lookups here because this scope can be used for kotlin packages too (if it doesn't contain toplevel properties nor functions).
        recordLookup(name, location)
        return deserializedPackageScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
    }

    override fun getContributedFunctions(name: Name, location: LookupLocation): List<SimpleFunctionDescriptor> {
        // We should track lookups here because this scope can be used for kotlin packages too (if it doesn't contain toplevel properties nor functions).
        recordLookup(name, location)
        return deserializedPackageScope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED) + super.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED)
    }

    override fun addExtraDescriptors(result: MutableSet<DeclarationDescriptor>,
                                     kindFilter: DescriptorKindFilter,
                                     nameFilter: (Name) -> Boolean) {
        result.addAll(deserializedPackageScope.getContributedDescriptors(kindFilter, nameFilter))
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
        c.components.samConversionResolver.resolveSamConstructor(ownerDescriptor) {
            getContributedClassifier(name, NoLookupLocation.FOR_ALREADY_TRACKED)
        }?.let { result.add(it) }
    }

    override fun getSubPackages() = subPackages()

    override fun getPropertyNames(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = listOf<Name>()

    // we don't use implementation from super which caches all descriptors and does not use filters
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptors(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
    }
}
