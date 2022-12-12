/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.load.kotlin.JvmMemberAnnotationsSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ClassLiteralValue
import org.jetbrains.kotlin.utils.ReusableByteArray

data class JvmMemberAndClassAnnotations(
    val memberAnnotations: List<JvmMemberAnnotation>,
    val classAnnotations: List<JvmAnnotationNode>
)

sealed class JvmMemberAnnotation(
    val name: Name,
    val desc: String,
    val annotations: List<JvmAnnotationNode>?
) {
    class ForMethod(
        name: Name, desc: String, annotations: List<JvmAnnotationNode>?,
        val paramAnnotations: List<List<JvmAnnotationNode>?>?
    ) : JvmMemberAnnotation(name, desc, annotations)

    class ForField(
        name: Name, desc: String,
        val initializer: Any?,
        annotations: List<JvmAnnotationNode>?
    ) : JvmMemberAnnotation(name, desc, annotations)
}

/**
 * Reads member and class annotations in a single class reader pass into an intermediate data structures that
 * can be lazily used later, without the need to retain the underlying byte array.
 */
fun readMemberAndClassAnnotations(
    traceName: String,
    kotlinBinaryClass: KotlinJvmBinaryClass,
    byteContent: ReusableByteArray?,
): JvmMemberAndClassAnnotations {
    val storage = JvmAnnotationNodeStorage()
    val classAnnotations = ArrayList<JvmAnnotationNode>()
    val memberAnnotations = ArrayList<JvmMemberAnnotation>()

    val visitor = object : KotlinJvmBinaryClass.AnnotationVisitor, KotlinJvmBinaryClass.MemberVisitor {
        override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            val node = JvmAnnotationNode(classId, source, storage)
            classAnnotations += node
            return node
        }

        override fun visitEnd() {}

        override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor =
            AnnotationVisitorForMethod(name, desc)

        override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor =
            AnnotationVisitorForField(name, desc, initializer)

        abstract inner class AnnotationVisitorBase(
            protected val name: Name,
            protected val desc: String
        ) : KotlinJvmBinaryClass.AnnotationVisitor {
            protected var annotations: ArrayList<JvmAnnotationNode>? = null

            override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                return JvmAnnotationNode(classId, source, storage).also { node ->
                    val result = this.annotations ?: ArrayList<JvmAnnotationNode>().also { this.annotations = it }
                    result += node
                }
            }
        }

        inner class AnnotationVisitorForMethod(
            name: Name, desc: String
        ) : AnnotationVisitorBase(name, desc), KotlinJvmBinaryClass.MethodAnnotationVisitor {
            private var paramAnnotations: ArrayList<ArrayList<JvmAnnotationNode>?>? = null

            override fun visitParameterAnnotation(
                index: Int,
                classId: ClassId,
                source: SourceElement
            ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                val params = this.paramAnnotations ?: ArrayList<ArrayList<JvmAnnotationNode>?>().also { this.paramAnnotations = it }
                while (index > params.lastIndex) params += null
                val pList = params[index] ?: ArrayList<JvmAnnotationNode>().also { params[index] = it }
                return JvmAnnotationNode(classId, source, storage).also { node ->
                    pList += node
                }
            }

            override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                // TODO: load annotation default values to properly support annotation instantiation feature
                return null
            }

            override fun visitEnd() {
                if (annotations != null || paramAnnotations != null) {
                    memberAnnotations += JvmMemberAnnotation.ForMethod(name, desc, annotations, paramAnnotations)
                }
            }
        }

        inner class AnnotationVisitorForField(
            name: Name, desc: String,
            private val initializer: Any?
        ) : AnnotationVisitorBase(name, desc) {
            override fun visitEnd() {
                if (annotations != null || initializer != null) {
                    memberAnnotations += JvmMemberAnnotation.ForField(name, desc, initializer, annotations)
                }
            }
        }
    }
    kotlinBinaryClass.visitMemberAndClassAnnotations(traceName, visitor, visitor, byteContent)
    return JvmMemberAndClassAnnotations(memberAnnotations, classAnnotations)
}

class JvmMemberAnnotations(
    private val memberAnnotations: List<JvmMemberAnnotation>
) : JvmMemberAnnotationsSource {
    override fun visitMemberAnnotations(
        traceName: String,
        memberVisitor: KotlinJvmBinaryClass.MemberVisitor,
        cachedContents: ReusableByteArray?
    ) {
        for (memberAnnotation in memberAnnotations) {
            when (memberAnnotation) {
                is JvmMemberAnnotation.ForMethod -> {
                    val annotationVisitor = memberVisitor.visitMethod(memberAnnotation.name, memberAnnotation.desc)
                    if (annotationVisitor != null) {
                        memberAnnotation.annotations?.forEach { a ->
                            a.accept(annotationVisitor.visitAnnotation(a.classId, a.source))
                        }
                        val params = memberAnnotation.paramAnnotations
                        if (params != null) {
                            for (index in params.indices) {
                                params[index]?.forEach { a ->
                                    a.accept(
                                        annotationVisitor.visitParameterAnnotation(index, a.classId, a.source)
                                    )
                                }
                            }
                        }
                    }
                }
                is JvmMemberAnnotation.ForField -> {
                    val annotationVisitor =
                        memberVisitor.visitField(memberAnnotation.name, memberAnnotation.desc, memberAnnotation.initializer)
                    if (annotationVisitor != null) {
                        memberAnnotation.annotations?.forEach { a ->
                            a.accept(annotationVisitor.visitAnnotation(a.classId, a.source))
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------------------------------------
// Efficient data structures to store annotations from KotlinJvmBinaryClass

enum class Op { Visit, VisitClassLiteral, VisitEnum, VisitAnnotation, VisitArray }

private val opValues = Op.values()
private const val LEVEL_SHIFT = 3
private const val OP_MASK = (1 shl LEVEL_SHIFT) - 1

private fun pack(op: Op, level: Int): Short = (op.ordinal or (level shl LEVEL_SHIFT)).toShort()
private fun unpackOp(op: Short): Op = opValues[(op.toInt() and OP_MASK)]
private fun unpackLevel(op: Short): Int = op.toInt().shr(LEVEL_SHIFT)

class JvmAnnotationNodeStorage {
    var ops = ShortArray(4)
    var objs = arrayOfNulls<Any?>(8)
    var opsIndex = 0
    var objsIndex = 0

    fun add(op: Op, level: Int) {
        if (opsIndex >= ops.size) ops = ops.copyOf(ops.size * 2)
        ops[opsIndex++] = pack(op, level)
    }

    fun add(obj: Any?) {
        if (objsIndex >= objs.size) objs = objs.copyOf(objs.size * 2)
        objs[objsIndex++] = obj
    }
}

abstract class JvmAnnotationNodeBase(
    val storage: JvmAnnotationNodeStorage,
    val level: Int
) {
    var opsIndexStart = -1
    var objsIndexStart = -1
    var opsIndexEnd = -1

    operator fun Op.unaryPlus() = storage.add(this)
    operator fun Any?.unaryPlus() = storage.add(this)

    fun start() {
        opsIndexStart = storage.opsIndex
        objsIndexStart = storage.objsIndex
    }

    fun visitEnd() {
        opsIndexEnd = storage.opsIndex
    }
}

fun JvmAnnotationNode(classId: ClassId, source: SourceElement, storage: JvmAnnotationNodeStorage): JvmAnnotationNode =
    JvmAnnotationNode(classId, source, storage, 1).apply { start() }

class JvmAnnotationNode(
    val classId: ClassId,
    val source: SourceElement,
    storage: JvmAnnotationNodeStorage,
    level: Int
) : JvmAnnotationNodeBase(storage, level), KotlinJvmBinaryClass.AnnotationArgumentVisitor {
    override fun visit(name: Name?, value: Any?) {
        +Op.Visit
        +name
        +value
    }

    override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
        +Op.VisitClassLiteral
        +name
        +value
    }

    override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
        +Op.VisitEnum
        +name
        +enumClassId
        +enumEntryName
    }

    override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val node = JvmAnnotationNode(classId, source, storage, level + 1)
        +Op.VisitAnnotation
        +name
        +node
        return node.apply { start() }
    }

    override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
        val node = JvmAnnotationArrayArgumentNode(source, storage, level + 1)
        +Op.VisitArray
        +name
        +node
        return node.apply { start() }
    }

    fun accept(visitor: KotlinJvmBinaryClass.AnnotationArgumentVisitor?) {
        if (visitor == null) return
        var i = objsIndexStart
        with(storage) {
            for (j in opsIndexStart until opsIndexEnd) {
                val op = ops[j]
                if (unpackLevel(op) != level) continue
                when (unpackOp(op)) {
                    Op.Visit -> visitor.visit(objs[i++] as Name?, objs[i++])
                    Op.VisitClassLiteral -> visitor.visitClassLiteral(objs[i++] as Name?, objs[i++] as ClassLiteralValue)
                    Op.VisitEnum -> visitor.visitEnum(objs[i++] as Name?, objs[i++] as ClassId, objs[i++] as Name)
                    Op.VisitAnnotation -> {
                        val name = objs[i++] as Name?
                        val node = objs[i++] as JvmAnnotationNode
                        node.accept(visitor.visitAnnotation(name, node.classId))
                    }
                    Op.VisitArray -> {
                        val name = objs[i++] as Name?
                        val node = objs[i++] as JvmAnnotationArrayArgumentNode
                        node.accept(visitor.visitArray(name))
                    }
                }
            }
        }
        visitor.visitEnd()
    }
}

class JvmAnnotationArrayArgumentNode(
    val source: SourceElement,
    storage: JvmAnnotationNodeStorage,
    level: Int
) : JvmAnnotationNodeBase(storage, level + 1), KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
    override fun visit(value: Any?) {
        +Op.Visit
        +value
    }

    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
        +Op.VisitEnum
        +enumClassId
        +enumEntryName
    }

    override fun visitClassLiteral(value: ClassLiteralValue) {
        +Op.VisitClassLiteral
        +value
    }

    override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        val node = JvmAnnotationNode(classId, source, storage, level + 1)
        +Op.VisitAnnotation
        +node
        return node.apply { start() }
    }

    fun accept(visitor: KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor?) {
        if (visitor == null) return
        var i = objsIndexStart
        with(storage) {
            for (j in opsIndexStart until opsIndexEnd) {
                val op = ops[j]
                if (unpackLevel(op) != level) continue
                when (unpackOp(op)) {
                    Op.Visit -> visitor.visit(objs[i++])
                    Op.VisitClassLiteral -> visitor.visitClassLiteral(objs[i++] as ClassLiteralValue)
                    Op.VisitEnum -> visitor.visitEnum(objs[i++] as ClassId, objs[i++] as Name)
                    Op.VisitAnnotation -> {
                        val node = objs[i++] as JvmAnnotationNode
                        node.accept(visitor.visitAnnotation(node.classId))
                    }
                    else -> error(opValues[ops[j].toInt()])
                }
            }
        }
        visitor.visitEnd()
    }
}