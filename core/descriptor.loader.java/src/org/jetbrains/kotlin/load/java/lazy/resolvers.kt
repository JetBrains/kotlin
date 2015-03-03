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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.mapToIndex

//TODO: (module refactoring) usages of this interface should be replaced by ModuleClassResolver
trait LazyJavaClassResolver {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor?
}

trait TypeParameterResolver {
    object EMPTY : TypeParameterResolver {
        override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? = null
    }

    fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor?
}

class LazyJavaTypeParameterResolver(
        private val c: LazyJavaResolverContext,
        private val containingDeclaration: DeclarationDescriptor,
        typeParameterOwner: JavaTypeParameterListOwner
) : TypeParameterResolver {
    private val typeParameters: Map<JavaTypeParameter, Int> = typeParameterOwner.getTypeParameters().mapToIndex()

    private val resolve = c.storageManager.createMemoizedFunctionWithNullableValues {
        (typeParameter: JavaTypeParameter) ->
        typeParameters[typeParameter]?.let { index ->
            LazyJavaTypeParameterDescriptor(c.child(this), typeParameter, index, containingDeclaration)
        }
    }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return resolve(javaTypeParameter) ?: c.typeParameterResolver.resolveTypeParameter(javaTypeParameter)
    }
}

data class JavaClassLookupResult(val jClass: JavaClass? = null, val kClass: ClassDescriptor? = null)

fun LazyJavaResolverContext.lookupBinaryClass(javaClass: JavaClass): ClassDescriptor? {
    val kotlinJvmBinaryClass = kotlinClassFinder.findKotlinClass(javaClass)
    return resolveBinaryClass(kotlinJvmBinaryClass)?.kClass
}

fun LazyJavaResolverContext.findClassInJava(classId: ClassId): JavaClassLookupResult {
    val kotlinClass = kotlinClassFinder.findKotlinClass(classId)
    val binaryClassResult = resolveBinaryClass(kotlinClass)
    if (binaryClassResult != null) return binaryClassResult

    val javaClass = finder.findClass(classId)
    if (javaClass == null) return JavaClassLookupResult()

    // Light classes are not proper binaries either
    if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) return JavaClassLookupResult()

    return JavaClassLookupResult(javaClass)

}

private fun LazyJavaResolverContext.resolveBinaryClass(kotlinClass: KotlinJvmBinaryClass?): JavaClassLookupResult? {
    if (kotlinClass == null) return null

    val header = kotlinClass.getClassHeader()
    if (!header.isCompatibleAbiVersion) {
        errorReporter.reportIncompatibleAbiVersion(kotlinClass, header.version)
    }
    else if (header.kind == KotlinClassHeader.Kind.CLASS) {
        val descriptor = deserializedDescriptorResolver.resolveClass(kotlinClass)
        if (descriptor != null) {
            return JavaClassLookupResult(kClass = descriptor)
        }
    }
    else {
        // This is a package or trait-impl or something like that
        return JavaClassLookupResult()
    }

    return null
}
