/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.utils.ReusableByteArray

interface KotlinJvmBinaryClass : JvmMemberAnnotationsSource {
    val classId: ClassId

    /**
     * @return path to the class file (to be reported to the user upon error)
     */
    val location: String

    val containingLibrary: String?
        get() = null

    fun visitClassAnnotations(traceName: String, annotationVisitor: AnnotationVisitor, cachedContents: ReusableByteArray?)

    fun visitMemberAndClassAnnotations(
        traceName: String,
        annotationVisitor: AnnotationVisitor?,
        memberVisitor: MemberVisitor,
        cachedContents: ReusableByteArray?
    )

    override fun visitMemberAnnotations(traceName: String, memberVisitor: MemberVisitor, cachedContents: ReusableByteArray?) {
        visitMemberAndClassAnnotations(traceName, null, memberVisitor, cachedContents)
    }

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

        fun visitAnnotationMemberDefaultValue(): AnnotationArgumentVisitor?
    }

    interface AnnotationArgumentVisitor {
        fun visit(name: Name?, value: Any?)

        fun visitClassLiteral(name: Name?, value: ClassLiteralValue)

        fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name)

        fun visitAnnotation(name: Name?, classId: ClassId): AnnotationArgumentVisitor?

        fun visitArray(name: Name?): AnnotationArrayArgumentVisitor?

        fun visitEnd()
    }

    interface AnnotationArrayArgumentVisitor {
        fun visit(value: Any?)

        fun visitEnum(enumClassId: ClassId, enumEntryName: Name)

        fun visitClassLiteral(value: ClassLiteralValue)

        fun visitAnnotation(classId: ClassId): AnnotationArgumentVisitor?

        fun visitEnd()
    }
}

/**
 * This interface provides ability to visit member annotation both from class bytes, via [KotlinJvmBinaryClass] as well as from
 * the previously parsed in-memory representation that does not involve keeping an array of bytes around, via
 * [org.jetbrains.kotlin.fir.java.deserialization.JvmMemberAnnotations].
 */
interface JvmMemberAnnotationsSource {
    fun visitMemberAnnotations(
        traceName: String,
        memberVisitor: KotlinJvmBinaryClass.MemberVisitor,
        cachedContents: ReusableByteArray?
    )
}