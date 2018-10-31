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

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue

interface KotlinJvmBinaryClass {
    val classId: ClassId

    /**
     * @return path to the class file (to be reported to the user upon error)
     */
    val location: String

    fun loadClassAnnotations(visitor: AnnotationVisitor, cachedContents: ByteArray?)

    fun visitMembers(visitor: MemberVisitor, cachedContents: ByteArray?)

    val classHeader: KotlinClassHeader

    interface MemberVisitor {
        // TODO: abstract signatures for methods and fields instead of ASM 'desc' strings?

        fun visitMethod(name: Name, desc: String): MethodAnnotationVisitor?

        fun visitField(name: Name, desc: String, initializer: Any?): AnnotationVisitor?
    }

    interface AnnotationVisitor {
        fun visitAnnotation(classId: ClassId, source: SourceElement): AnnotationArgumentVisitor?

        fun visitEnd()
    }

    interface MethodAnnotationVisitor : AnnotationVisitor {
        fun visitParameterAnnotation(index: Int, classId: ClassId, source: SourceElement): AnnotationArgumentVisitor?
    }

    interface AnnotationArgumentVisitor {
        fun visit(name: Name?, value: Any?)

        fun visitClassLiteral(name: Name, value: ClassLiteralValue)

        fun visitEnum(name: Name, enumClassId: ClassId, enumEntryName: Name)

        fun visitAnnotation(name: Name, classId: ClassId): AnnotationArgumentVisitor?

        fun visitArray(name: Name): AnnotationArrayArgumentVisitor?

        fun visitEnd()
    }

    interface AnnotationArrayArgumentVisitor {
        fun visit(value: Any?)

        fun visitEnum(enumClassId: ClassId, enumEntryName: Name)

        fun visitClassLiteral(value: ClassLiteralValue)

        fun visitEnd()
    }
}
