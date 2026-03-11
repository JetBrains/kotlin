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
     * @param tryResolve callback that checks if a fully qualified class name exists
     * @return the resolved ClassId, or null if resolution fails
     */
    fun resolveEnumClass(tryResolve: (String) -> Boolean): ClassId? = enumClassId
}

interface JavaClassObjectAnnotationArgument : JavaAnnotationArgument {
    fun getReferencedType(): JavaType
}

interface JavaAnnotationAsAnnotationArgument : JavaAnnotationArgument {
    fun getAnnotation(): JavaAnnotation
}

interface JavaUnknownAnnotationArgument : JavaAnnotationArgument
