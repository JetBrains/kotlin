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

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.ReusableByteArray
import org.jetbrains.kotlin.utils.readToReusableByteArrayRef
import java.io.File

class LocalFileKotlinClass private constructor(
    private val file: File,
    private val fileContentsRef: ReusableByteArray, // permanently counter as a reference (release never called)
    className: ClassId,
    classVersion: Int,
    classHeader: KotlinClassHeader,
    innerClasses: InnerClassesInfo
) : FileBasedKotlinClass(className, classVersion, classHeader, innerClasses) {

    companion object {
        fun create(file: File): LocalFileKotlinClass? {
            // FileContentsRef will be permanently stored inside LocalFileKotlinClass
            val fileContentsRef = file.inputStream().readToReusableByteArrayRef(file.length())
            return create(fileContentsRef) { className, classVersion, classHeader, innerClasses ->
                LocalFileKotlinClass(file, fileContentsRef, className, classVersion, classHeader, innerClasses)
            }
        }
    }

    val className: JvmClassName by lazy { JvmClassName.byClassId(classId) }

    override val location: String
        get() = file.absolutePath

    public override fun getFileContentsRef(): ReusableByteArray = fileContentsRef.also { fileContentsRef.addRef() }

    override fun hashCode(): Int = file.hashCode()
    override fun equals(other: Any?): Boolean = other is LocalFileKotlinClass && file == other.file
    override fun toString(): String = "${this::class.java}: $file"
}
