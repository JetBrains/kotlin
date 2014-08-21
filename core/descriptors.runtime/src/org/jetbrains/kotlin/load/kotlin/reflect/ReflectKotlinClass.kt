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

package org.jetbrains.kotlin.load.kotlin.reflect

import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor

public class ReflectKotlinClass(private val klass: Class<*>) : KotlinJvmBinaryClass {
    private val classHeader: KotlinClassHeader

    {
        val headerReader = ReadKotlinClassHeaderAnnotationVisitor()
        loadClassAnnotations(headerReader)
        val header = headerReader.createHeader()

        if (header == null) {
            // This exception must be caught in the caller
            throw NotAKotlinClass()
        }

        classHeader = header
    }

    class NotAKotlinClass : RuntimeException()

    override fun getClassId(): ClassId = klass.classId

    override fun loadClassAnnotations(visitor: KotlinJvmBinaryClass.AnnotationVisitor) {
        // TODO
        visitor.visitEnd()
    }

    override fun visitMembers(visitor: KotlinJvmBinaryClass.MemberVisitor) {
        // TODO
    }

    override fun getClassHeader() = classHeader
}
