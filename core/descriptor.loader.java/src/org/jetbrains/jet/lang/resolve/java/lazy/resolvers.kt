/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.lazy

import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaTypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils

trait LazyJavaClassResolver {
    fun resolveClass(javaClass: JavaClass): ClassDescriptor?
    fun resolveClassByFqName(fqName: FqName): ClassDescriptor?
}

trait TypeParameterResolver {
    class object {
        object EMPTY : TypeParameterResolver {
            override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? = null
        }
    }

    fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor?
}

class LazyJavaTypeParameterResolver(
        val c: LazyJavaResolverContextWithTypes,
        private val containingDeclaration: DeclarationDescriptor,
        private val typeParameters: Set<JavaTypeParameter>
) : TypeParameterResolver {

    private val resolve = c.storageManager.createMemoizedFunctionWithNullableValues {
                (javaTypeParameter: JavaTypeParameter) ->
                if (javaTypeParameter in typeParameters)
                    LazyJavaTypeParameterDescriptor(
                            c.withTypes(this),
                            javaTypeParameter,
                            containingDeclaration
                    )
                else null
            }

    override fun resolveTypeParameter(javaTypeParameter: JavaTypeParameter): TypeParameterDescriptor? {
        return resolve(javaTypeParameter) ?: c.typeParameterResolver.resolveTypeParameter(javaTypeParameter)
    }
}

fun LazyJavaResolverContext.findJavaClass(fqName: FqName): JavaClass? = findClassInJava(fqName).jClass

data class JavaClassLookupResult(val jClass: JavaClass? = null, val kClass: ClassDescriptor? = null)

fun LazyJavaResolverContext.findClassInJava(fqName: FqName): JavaClassLookupResult {
    // TODO: this should be governed by module separation logic
    // Do not look for JavaClasses for Kotlin binaries & built-ins
    if (DescriptorResolverUtils.getKotlinBuiltinClassDescriptor(fqName) != null) {
        return JavaClassLookupResult()
    }

    val kotlinClass = kotlinClassFinder.findKotlinClass(fqName)
    val header = kotlinClass?.getClassHeader()
    if (kotlinClass != null && header != null) {
        if (header.getKind() == KotlinClassHeader.Kind.CLASS) {
            val descriptor = packageFragmentProvider.resolveKotlinBinaryClass(kotlinClass)
            if (descriptor != null) {
                return JavaClassLookupResult(kClass = descriptor)
            }
        }
        else if (header.getKind() == KotlinClassHeader.Kind.INCOMPATIBLE_ABI_VERSION) {
            errorReporter.reportIncompatibleAbiVersion(kotlinClass, header.getVersion())
        }
        else {
            // This is a package or trait-impl or something like that
            return JavaClassLookupResult()
        }
    }

    val javaClass = finder.findClass(fqName)
    if (javaClass == null) return JavaClassLookupResult()

    // Light classes are not proper binaries either
    if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) return JavaClassLookupResult()

    return JavaClassLookupResult(javaClass)

}