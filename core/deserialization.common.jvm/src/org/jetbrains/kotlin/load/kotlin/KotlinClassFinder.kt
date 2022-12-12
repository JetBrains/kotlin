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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.utils.ReusableByteArray

interface KotlinClassFinder : KotlinMetadataFinder {
    /**
     * **Resource ref counting**: Returns result with a contentRef reference that must be released after use.
     */
    fun findKotlinClassOrContent(classId: ClassId): Result?

    /**
     * **Resource ref counting**: Returns result with a contentRef reference that must be released after use.
     */
    fun findKotlinClassOrContent(javaClass: JavaClass): Result?

    sealed class Result {
        fun toKotlinJvmBinaryClass(): KotlinJvmBinaryClass? = (this as? KotlinClass)?.kotlinJvmBinaryClass

        /***
         * This reference is counted in reusable byte array refCount. Call [ReusableByteArray.release] on it when done working with result.
         */
        abstract val contentRef: ReusableByteArray?

        class KotlinClass(val kotlinJvmBinaryClass: KotlinJvmBinaryClass, override val contentRef: ReusableByteArray? = null) : Result() {
            operator fun component1(): KotlinJvmBinaryClass = kotlinJvmBinaryClass
            operator fun component2(): ReusableByteArray? = contentRef
        }

        class ClassFileContent(override val contentRef: ReusableByteArray) : Result()
    }
}

fun KotlinClassFinder.findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
    val result = findKotlinClassOrContent(classId)
    return result?.toKotlinJvmBinaryClass().also { result?.contentRef?.release() }
}

fun KotlinClassFinder.findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
    val result = findKotlinClassOrContent(javaClass)
    return result?.toKotlinJvmBinaryClass().also { result?.contentRef?.release() }
}
