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

import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.lang.resolve.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.java.structure.JavaTypeParameter
import org.jetbrains.jet.lang.resolve.java.JavaClassFinder
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation
import org.jetbrains.jet.lang.resolve.java.lazy.descriptors.LazyJavaAnnotationDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor

open class LazyJavaResolverContext(
        val storageManager: StorageManager,
        val finder: JavaClassFinder,
        val javaClassResolver: LazyJavaClassResolver
)

fun LazyJavaResolverContext.withTypes(
        typeParameterResolver: TypeParameterResolver = TypeParameterResolver.EMPTY
)  =  LazyJavaResolverContextWithTypes(
        storageManager,
        finder,
        javaClassResolver,
        LazyJavaTypeResolver(this, typeParameterResolver),
        typeParameterResolver)

class LazyJavaResolverContextWithTypes(
        storageManager: StorageManager,
        finder: JavaClassFinder,
        javaClassResolver: LazyJavaClassResolver,
        val typeResolver: LazyJavaTypeResolver,
        val typeParameterResolver: TypeParameterResolver
) : LazyJavaResolverContext(storageManager, finder, javaClassResolver)

fun LazyJavaResolverContextWithTypes.child(
        containingDeclaration: DeclarationDescriptor,
        typeParameters: Set<JavaTypeParameter>
): LazyJavaResolverContextWithTypes = this.withTypes(LazyJavaTypeParameterResolver(this, containingDeclaration, typeParameters))

fun LazyJavaResolverContextWithTypes.resolveAnnotations(javaAnnotations: Collection<JavaAnnotation>): List<AnnotationDescriptor>
        = javaAnnotations.map {jAnnotation -> LazyJavaAnnotationDescriptor(this, jAnnotation)}