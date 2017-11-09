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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.lazy.LazyJavaResolverContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.storage.NullableLazyValue
import org.jetbrains.kotlin.utils.alwaysTrue
import java.util.*

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

    private val classes = c.storageManager.createMemoizedFunctionWithNullableValues<FindClassRequest, ClassDescriptor> classByRequest@{ request ->
        val requestClassId = ClassId(ownerDescriptor.fqName, request.name)

        val kotlinBinaryClass =
                // These branches should be semantically equal, but the first one could be faster
                if (request.javaClass != null)
                    c.components.kotlinClassFinder.findKotlinClass(request.javaClass)
                else
                    c.components.kotlinClassFinder.findKotlinClass(requestClassId)

        val classId = kotlinBinaryClass?.classId
        // Nested/local classes can be found when running in CLI in case when request.name looks like 'Outer$Inner'
        // It happens because KotlinClassFinder searches through a file-based index that does not differ classes containing $-sign and nested ones
        if (classId != null && (classId.isNestedClass || classId.isLocal)) return@classByRequest null

        val kotlinResult = resolveKotlinBinaryClass(kotlinBinaryClass)

        when (kotlinResult) {
            is KotlinClassLookupResult.Found -> kotlinResult.descriptor
            is KotlinClassLookupResult.SyntheticClass -> null
            is KotlinClassLookupResult.NotFound -> {
                val javaClass = request.javaClass ?: c.components.finder.findClass(requestClassId)

                if (javaClass?.lightClassOriginKind == LightClassOriginKind.BINARY) {
                    throw IllegalStateException(
                            "Couldn't find kotlin binary class for light class created by kotlin binary file\n" +
                            "JavaClass: $javaClass\n" +
                            "ClassId: $requestClassId\n" +
                            "findKotlinClass(JavaClass) = ${c.components.kotlinClassFinder.findKotlinClass(javaClass)}\n" +
                            "findKotlinClass(ClassId) = ${c.components.kotlinClassFinder.findKotlinClass(requestClassId)}\n"
                    )
                }

                val actualFqName = javaClass?.fqName
                if (actualFqName == null || actualFqName.isRoot || actualFqName.parent() != ownerDescriptor.fqName)
                    null
                else
                    LazyJavaClassDescriptor(c, ownerDescriptor, javaClass)
            }
        }
    }

    private sealed class KotlinClassLookupResult {
        class Found(val descriptor: ClassDescriptor) : KotlinClassLookupResult()
        object NotFound : KotlinClassLookupResult()
        object SyntheticClass : KotlinClassLookupResult()
    }

    private fun resolveKotlinBinaryClass(kotlinClass: KotlinJvmBinaryClass?): KotlinClassLookupResult =
            when {
                kotlinClass == null -> {
                    KotlinClassLookupResult.NotFound
                }
                kotlinClass.classHeader.kind == KotlinClassHeader.Kind.CLASS -> {
                    val descriptor = c.components.deserializedDescriptorResolver.resolveClass(kotlinClass)
                    if (descriptor != null) KotlinClassLookupResult.Found(descriptor) else KotlinClassLookupResult.NotFound
                }
                else -> {
                    // This is a package or interface DefaultImpls or something like that
                    KotlinClassLookupResult.SyntheticClass
                }
            }

    // javaClass here is only for sake of optimizations
    private class FindClassRequest(val name: Name, val javaClass: JavaClass?) {
        override fun equals(other: Any?) = other is FindClassRequest && name == other.name

        override fun hashCode() = name.hashCode()
    }

    override fun getContributedClassifier(name: Name, location: LookupLocation) = findClassifier(name, null)

    private fun findClassifier(name: Name, javaClass: JavaClass?): ClassDescriptor? {
        if (!SpecialNames.isSafeIdentifier(name)) return null

        val knownClassNamesInPackage = knownClassNamesInPackage()
        if (javaClass == null && knownClassNamesInPackage != null && name.asString() !in knownClassNamesInPackage) {
            return null
        }

        return classes(FindClassRequest(name, javaClass))
    }

    internal fun findClassifierByJavaClass(javaClass: JavaClass) = findClassifier(javaClass.name, javaClass)

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = emptyList()

    override fun computeMemberIndex(): DeclaredMemberIndex = DeclaredMemberIndex.Empty

    override fun computeClassNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> {
        // neither objects nor enum members can be in java package
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)) return emptySet()

        val knownClassNamesInPackage = knownClassNamesInPackage()
        if (knownClassNamesInPackage != null) return knownClassNamesInPackage.mapTo(HashSet()) { Name.identifier(it) }

        return jPackage.getClasses(nameFilter ?: alwaysTrue()).mapNotNullTo(linkedSetOf()) { klass ->
            if (klass.lightClassOriginKind == LightClassOriginKind.SOURCE) null else klass.name
        }
    }

    override fun computeFunctionNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?): Set<Name> {
        return emptySet()
    }

    override fun computeNonDeclaredFunctions(result: MutableCollection<SimpleFunctionDescriptor>, name: Name) {
    }

    override fun computePropertyNames(kindFilter: DescriptorKindFilter, nameFilter: ((Name) -> Boolean)?) = emptySet<Name>()

    // we don't use implementation from super which caches all descriptors and does not use filters
    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        return computeDescriptors(kindFilter, nameFilter, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
    }
}
