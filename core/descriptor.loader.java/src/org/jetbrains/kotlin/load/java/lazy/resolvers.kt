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

package org.jetbrains.kotlin.load.java.lazy

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameterListOwner
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.utils.mapToIndex

//TODO: (module refactoring) usages of this interface should be replaced by ModuleClassResolver
interface LazyJavaClassResolver {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor?
}

interface TypeParameterResolver {
    object EMPTY : TypeParameterResolver {
        override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? = null
    }

    fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor?
}

class LazyJavaTypeParameterResolver(
        private val c: LazyJavaResolverContext,
        private val containingDeclaration: DeclarationDescriptor,
        typeParameterOwner: JavaTypeParameterListOwner,
        private val typeParametersIndexOffset: Int
) : TypeParameterResolver {
    private val typeParameters: Map<JavaTypeParameter, Int> = typeParameterOwner.typeParameters.mapToIndex()

    private val resolve = c.storageManager.createMemoizedFunctionWithNullableValues {
        typeParameter: JavaTypeParameter ->
        typeParameters[typeParameter]?.let { index ->
            LazyJavaTypeParameterDescriptor(c.child(this), typeParameter, typeParametersIndexOffset + index, containingDeclaration)
        }
    }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return resolve(javaTypeParameter) ?: c.typeParameterResolver.resolveTypeParameter(javaTypeParameter)
    }
}

sealed class KotlinClassLookupResult {
    class Found(val descriptor: ClassDescriptor): KotlinClassLookupResult()
    object NotFound : KotlinClassLookupResult()
    object SyntheticClass : KotlinClassLookupResult()
}

fun LazyJavaResolverContext.resolveKotlinBinaryClass(kotlinClass: KotlinJvmBinaryClass?): KotlinClassLookupResult {
    if (kotlinClass == null) return KotlinClassLookupResult.NotFound

    val header = kotlinClass.classHeader
    return when {
        !header.metadataVersion.isCompatible() -> {
            components.errorReporter.reportIncompatibleMetadataVersion(kotlinClass.classId, kotlinClass.location, header.metadataVersion)
            KotlinClassLookupResult.NotFound
        }
        header.kind == KotlinClassHeader.Kind.CLASS -> {
            val descriptor = components.deserializedDescriptorResolver.resolveClass(kotlinClass)
            if (descriptor != null) KotlinClassLookupResult.Found(descriptor) else KotlinClassLookupResult.NotFound
        }
        else -> {
            // This is a package or trait-impl or something like that
            KotlinClassLookupResult.SyntheticClass
        }
    }
}
