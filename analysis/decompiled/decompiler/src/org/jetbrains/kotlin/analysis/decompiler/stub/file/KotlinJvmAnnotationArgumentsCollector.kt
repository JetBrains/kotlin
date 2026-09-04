/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.impl.AnnotationData
import org.jetbrains.kotlin.psi.stubs.impl.EnumData
import org.jetbrains.kotlin.psi.stubs.impl.KClassData
import org.jetbrains.kotlin.psi.stubs.impl.createConstantValue
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * Collects annotation arguments from JVM class files into a [Name]-keyed map of [ConstantValue]s.
 *
 * Handles all argument kinds: primitives, strings, enums, class literals, nested annotations, and arrays.
 * Nested annotations and annotation arrays are collected recursively.
 *
 * Subclasses override [visitEnd] to consume the collected [args] map.
 *
 * @see KotlinJvmBinaryClass.AnnotationArgumentVisitor
 */
internal open class KotlinJvmAnnotationArgumentsCollector : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
    protected val args = mutableMapOf<Name, ConstantValue<*>>()

    private fun nameOrSpecial(name: Name?): Name {
        return name ?: Name.special("<no_name>")
    }

    override fun visit(name: Name?, value: Any?) {
        val constantValue = createConstantValue(value)
        args[nameOrSpecial(name)] = constantValue
    }

    override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
        args[nameOrSpecial(name)] = createConstantValue(KClassData(value.classId, value.arrayNestedness))
    }

    override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
        args[nameOrSpecial(name)] = createConstantValue(EnumData(enumClassId, enumEntryName))
    }

    override fun visitAnnotation(
        name: Name?,
        classId: ClassId
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        val visitor = KotlinJvmAnnotationArgumentsCollector()
        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
            override fun visitEnd() {
                args[nameOrSpecial(name)] = createConstantValue(AnnotationData(classId, visitor.args))
            }
        }
    }

    override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
        return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
            private val elements = mutableListOf<Any>()

            override fun visit(value: Any?) {
                elements.addIfNotNull(value)
            }

            override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                elements.add(EnumData(enumClassId, enumEntryName))
            }

            override fun visitClassLiteral(value: ClassLiteralValue) {
                elements.add(KClassData(value.classId, value.arrayNestedness))
            }

            override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                val visitor = KotlinJvmAnnotationArgumentsCollector()
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        elements.addIfNotNull(AnnotationData(classId, visitor.args))
                    }
                }
            }

            override fun visitEnd() {
                args[nameOrSpecial(name)] = createConstantValue(elements.toTypedArray())
            }
        }
    }

    override fun visitEnd() {}
}
