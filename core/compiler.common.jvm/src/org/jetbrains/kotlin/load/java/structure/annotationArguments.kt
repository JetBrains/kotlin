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

package org.jetbrains.kotlin.load.java.structure

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface JavaAnnotationArgument {
    val name: Name?
}

interface JavaLiteralAnnotationArgument : JavaAnnotationArgument {
    val value: Any?
}

interface JavaArrayAnnotationArgument : JavaAnnotationArgument {
    fun getElements(): List<JavaAnnotationArgument>
}

interface JavaEnumValueAnnotationArgument : JavaAnnotationArgument {
    val enumClassId: ClassId?
    val entryName: Name?

    /**
     * Whether the enum class reference is already resolved (fully qualified or imported).
     * If false, FIR should use [resolveEnumClass] for resolution.
     */
    val isResolved: Boolean get() = true

    /**
     * Resolves the enum class using the provided callback.
     * Called when [isResolved] is false or [enumClassId] may be incorrect for nested classes.
     *
     * @param tryResolve callback that checks if a ClassId exists
     * @return the resolved ClassId, or null if resolution fails
     */
    fun resolveEnumClass(tryResolve: (ClassId) -> Boolean): ClassId? = enumClassId

    /**
     * Whether this argument might denote a Kotlin/Java compile-time constant rather than a real
     * enum entry. PSI/binary classifiers can statically tell `KConstsKt.WARNING` (a const) from
     * `RetentionPolicy.RUNTIME` (an enum entry) at structure-build time and produce the
     * appropriate `JavaLiteralAnnotationArgument` / `JavaEnumValueAnnotationArgument`
     * respectively, so PSI never needs the const-field fallback in FIR (default `false`).
     * java-direct can't disambiguate at parse time and emits `JavaEnumValueAnnotationArgument`
     * for both cases, so it overrides this to `true` to opt into the const-field fallback.
     */
    val couldBeConstReference: Boolean get() = false
}

interface JavaClassObjectAnnotationArgument : JavaAnnotationArgument {
    fun getReferencedType(): JavaType
}

interface JavaAnnotationAsAnnotationArgument : JavaAnnotationArgument {
    fun getAnnotation(): JavaAnnotation
}

interface JavaUnknownAnnotationArgument : JavaAnnotationArgument
