/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure

import org.jetbrains.kotlin.builtins.PrimitiveType

interface JavaType : ListBasedJavaAnnotationOwner {
    /**
     * Filters annotations to only include TYPE_USE annotations.
     * 
     * This is used when converting Java types to FIR - only annotations with 
     * `@Target(ElementType.TYPE_USE)` should appear on types.
     * 
     * The default implementation returns all annotations unchanged, assuming that
     * the implementation has already filtered them (as javac-wrapper does at the
     * Java structure level).
     * 
     * Implementations that don't pre-filter (like java-direct) should override this
     * to use the callback for filtering.
     * 
     * @param isTypeUseAnnotation callback that checks if a given annotation class has TYPE_USE target.
     *        The callback receives the fully qualified annotation class name and returns true if it's TYPE_USE.
     * @return collection of annotations that are TYPE_USE annotations
     */
    fun filterTypeUseAnnotations(isTypeUseAnnotation: (String) -> Boolean): Collection<JavaAnnotation> {
        // Default: return annotations as-is (javac-wrapper already filters at Java structure level)
        return annotations
    }
}

interface JavaArrayType : JavaType {
    val componentType: JavaType
}

interface JavaClassifierType : JavaType {
    val classifier: JavaClassifier?
    val typeArguments: List<JavaType?>

    val isRaw: Boolean

    val classifierQualifiedName: String
    val presentableText: String

    val isResolved: Boolean
        get() = true

    fun resolve(tryResolve: (String) -> Boolean): String? = null
}

interface JavaPrimitiveType : JavaType {
    /** `null` means the `void` type. */
    val type: PrimitiveType?
}

interface JavaWildcardType : JavaType {
    val bound: JavaType?
    val isExtends: Boolean
}

fun JavaType?.isSuperWildcard(): Boolean = (this as? JavaWildcardType)?.let { it.bound != null && !it.isExtends } ?: false
