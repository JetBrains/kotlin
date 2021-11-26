/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.org.objectweb.asm.*

class BytecodeListingTextCollectingVisitor(
    val filter: Filter,
    val withSignatures: Boolean,
    api: Int = Opcodes.API_VERSION,
    val withAnnotations: Boolean = true
) : ClassVisitor(api) {
    companion object {
        @JvmOverloads
        fun getText(
            factory: ClassFileFactory,
            filter: Filter = Filter.EMPTY,
            withSignatures: Boolean = false,
            withAnnotations: Boolean = true
        ) = factory.getClassFiles()
            .sortedBy { it.relativePath }
            .mapNotNull {
                val cr = ClassReader(it.asByteArray())
                val visitor = BytecodeListingTextCollectingVisitor(filter, withSignatures, withAnnotations = withAnnotations)
                cr.accept(visitor, ClassReader.SKIP_CODE)

                if (!filter.shouldWriteClass(cr.access, cr.className)) null else visitor.text
            }.joinToString("\n\n", postfix = "\n")

        private val CLASS_OR_FIELD_OR_METHOD = setOf(ModifierTarget.CLASS, ModifierTarget.FIELD, ModifierTarget.METHOD)
        private val CLASS_OR_METHOD = setOf(ModifierTarget.CLASS, ModifierTarget.METHOD)
        private val FIELD_ONLY = setOf(ModifierTarget.FIELD)
        private val METHOD_ONLY = setOf(ModifierTarget.METHOD)
        private val FIELD_OR_METHOD = setOf(ModifierTarget.FIELD, ModifierTarget.METHOD)

        // TODO ACC_MANDATED - requires reading Parameters attribute, which we don't generate by default
        internal val MODIFIERS =
            arrayOf(
                Modifier("public", Opcodes.ACC_PUBLIC, CLASS_OR_FIELD_OR_METHOD),
                Modifier("protected", Opcodes.ACC_PROTECTED, CLASS_OR_FIELD_OR_METHOD),
                Modifier("private", Opcodes.ACC_PRIVATE, CLASS_OR_FIELD_OR_METHOD),
                Modifier("synthetic", Opcodes.ACC_SYNTHETIC, CLASS_OR_FIELD_OR_METHOD),
                Modifier("bridge", Opcodes.ACC_BRIDGE, METHOD_ONLY),
                Modifier("volatile", Opcodes.ACC_VOLATILE, FIELD_ONLY),
                Modifier("synchronized", Opcodes.ACC_SYNCHRONIZED, METHOD_ONLY),
                Modifier("varargs", Opcodes.ACC_VARARGS, METHOD_ONLY),
                Modifier("transient", Opcodes.ACC_TRANSIENT, FIELD_ONLY),
                Modifier("native", Opcodes.ACC_NATIVE, METHOD_ONLY),
                Modifier("deprecated", Opcodes.ACC_DEPRECATED, CLASS_OR_FIELD_OR_METHOD),
                Modifier("final", Opcodes.ACC_FINAL, CLASS_OR_FIELD_OR_METHOD),
                Modifier("strict", Opcodes.ACC_STRICT, METHOD_ONLY),
                Modifier("enum", Opcodes.ACC_ENUM, FIELD_ONLY), // ACC_ENUM modifier on class is handled in 'classOrInterface'
                Modifier("abstract", Opcodes.ACC_ABSTRACT, CLASS_OR_METHOD, excludedMask = Opcodes.ACC_INTERFACE),
                Modifier("static", Opcodes.ACC_STATIC, FIELD_OR_METHOD)
            )
    }

    interface Filter {
        fun shouldWriteClass(access: Int, name: String): Boolean
        fun shouldWriteMethod(access: Int, name: String, desc: String): Boolean
        fun shouldWriteField(access: Int, name: String, desc: String): Boolean
        fun shouldWriteInnerClass(name: String, outerName: String?, innerName: String?): Boolean

        object EMPTY : Filter {
            override fun shouldWriteClass(access: Int, name: String) = true
            override fun shouldWriteMethod(access: Int, name: String, desc: String) = true
            override fun shouldWriteField(access: Int, name: String, desc: String) = true
            override fun shouldWriteInnerClass(name: String, outerName: String?, innerName: String?) = true
        }

        object ForCodegenTests : Filter {
            override fun shouldWriteClass(access: Int, name: String): Boolean = !name.startsWith("helpers/")
            override fun shouldWriteMethod(access: Int, name: String, desc: String): Boolean = true
            override fun shouldWriteField(access: Int, name: String, desc: String): Boolean = true
            override fun shouldWriteInnerClass(name: String, outerName: String?, innerName: String?): Boolean = true
        }
    }

    private class Declaration(val text: String, val annotations: MutableList<String> = arrayListOf())

    private val declarationsInsideClass = arrayListOf<Declaration>()
    private val classAnnotations = arrayListOf<String>()
    private var className = ""
    private var classAccess = 0
    private var classSignature: String? = ""

    private fun addAnnotation(annotation: String) {
        declarationsInsideClass.last().annotations.add("@$annotation ")
    }

    private fun addModifier(text: String, list: MutableList<String>) {
        list.add("$text ")
    }

    internal enum class ModifierTarget {
        CLASS, FIELD, METHOD
    }

    internal class Modifier(
        val text: String,
        private val mask: Int,
        private val applicableTo: Set<ModifierTarget>,
        private val excludedMask: Int = 0
    ) {
        fun hasModifier(access: Int, target: ModifierTarget) =
            access and mask != 0 &&
                    access and excludedMask == 0 &&
                    applicableTo.contains(target)
    }

    private fun handleModifiers(
        target: ModifierTarget,
        access: Int,
        list: MutableList<String> = declarationsInsideClass.last().annotations
    ) {
        for (modifier in MODIFIERS) {
            if (modifier.hasModifier(access, target)) {
                addModifier(modifier.text, list)
            }
        }
    }

    private fun getModifiers(target: ModifierTarget, access: Int) =
        MODIFIERS.filter { it.hasModifier(access, target) }.joinToString(separator = " ") { it.text }

    private fun classOrInterface(access: Int): String {
        return when {
            access and Opcodes.ACC_ANNOTATION != 0 -> "annotation class"
            access and Opcodes.ACC_ENUM != 0 -> "enum class"
            access and Opcodes.ACC_INTERFACE != 0 -> "interface"
            else -> "class"
        }
    }

    val text: String
        get() = StringBuilder().apply {
            if (classAnnotations.isNotEmpty()) {
                append(classAnnotations.joinToString("\n", postfix = "\n"))
            }
            arrayListOf<String>().apply { handleModifiers(ModifierTarget.CLASS, classAccess, this) }.forEach { append(it) }
            append(classOrInterface(classAccess))
            if (withSignatures) {
                append("<$classSignature> ")
            }
            append(" ")
            append(className)
            if (declarationsInsideClass.isNotEmpty()) {
                append(" {\n")
                for (declaration in declarationsInsideClass.sortedBy { it.text }) {
                    append("    ").append(declaration.annotations.joinToString("")).append(declaration.text).append("\n")
                }
                append("}")
            }
        }.toString()

    override fun visitSource(source: String?, debug: String?) {
        if (source != null) {
            declarationsInsideClass.add(Declaration("// source: '$source'"))
        } else {
            declarationsInsideClass.add(Declaration("// source: null"))
        }
    }

    override fun visitPermittedSubclass(permittedSubclass: String?) {
        if (permittedSubclass != null) {
            declarationsInsideClass.add(Declaration("permittedSubclass: $permittedSubclass"))
        }
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (!filter.shouldWriteMethod(access, name, desc)) {
            return null
        }

        val returnType = Type.getReturnType(desc).className
        val parameterTypes = Type.getArgumentTypes(desc).map { it.className }
        val methodAnnotations = arrayListOf<String>()
        val parameterAnnotations = hashMapOf<Int, MutableList<String>>()

        handleModifiers(ModifierTarget.METHOD, access, methodAnnotations)
        val methodParamCount = Type.getArgumentTypes(desc).size

        return object : MethodVisitor(Opcodes.API_VERSION) {
            private var visibleAnnotableParameterCount = methodParamCount
            private var invisibleAnnotableParameterCount = methodParamCount

            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                visitAnnotationImpl { args ->
                    methodAnnotations += "@" + renderAnnotation(desc, args) + " "
                }

            override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? =
                visitAnnotationImpl { args ->
                    parameterAnnotations.getOrPut(
                        parameter + methodParamCount - (if (visible) visibleAnnotableParameterCount else invisibleAnnotableParameterCount)
                    ) { arrayListOf() }.add("@" + renderAnnotation(desc, args) + " ")
                }

            override fun visitEnd() {
                val parameterWithAnnotations = parameterTypes.mapIndexed { index, parameter ->
                    val annotations = parameterAnnotations.getOrElse(index, { emptyList() }).joinToString("")
                    "${annotations}p$index: $parameter"
                }.joinToString()
                val signatureIfRequired = if (withSignatures) "<$signature> " else ""
                declarationsInsideClass.add(
                    Declaration("${signatureIfRequired}method $name($parameterWithAnnotations): $returnType", methodAnnotations)
                )
                super.visitEnd()
            }

            @Suppress("NOTHING_TO_OVERRIDE")
            override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
                if (visible)
                    visibleAnnotableParameterCount = parameterCount
                else {
                    invisibleAnnotableParameterCount = parameterCount
                }
            }
        }
    }

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        if (!filter.shouldWriteField(access, name, desc)) {
            return null
        }

        val type = Type.getType(desc).className
        val fieldSignature = if (withSignatures) "<$signature> " else ""
        val fieldDeclaration = Declaration("field $fieldSignature$name: $type")
        declarationsInsideClass.add(fieldDeclaration)
        handleModifiers(ModifierTarget.FIELD, access)
        if (access and Opcodes.ACC_VOLATILE != 0) addModifier("volatile", fieldDeclaration.annotations)

        return object : FieldVisitor(Opcodes.API_VERSION) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                visitAnnotationImpl { args ->
                    addAnnotation(renderAnnotation(desc, args))
                }
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
        visitAnnotationImpl { args ->
            classAnnotations.add("@" + renderAnnotation(desc, args))
        }

    private fun visitAnnotationImpl(end: (List<String>) -> Unit): AnnotationVisitor? =
        if (!withAnnotations) null else object : AnnotationVisitor(Opcodes.API_VERSION) {
            private val arguments = mutableListOf<String>()

            override fun visit(name: String?, value: Any) {
                val rendered = when (value) {
                    is String -> "\"$value\""
                    is Type -> "${value.className}::class"
                    is BooleanArray -> value.contentToString()
                    is CharArray -> value.contentToString()
                    is ByteArray -> value.contentToString()
                    is ShortArray -> value.contentToString()
                    is IntArray -> value.contentToString()
                    is FloatArray -> value.contentToString()
                    is LongArray -> value.contentToString()
                    is DoubleArray -> value.contentToString()
                    else -> value.toString()
                }
                arguments +=
                    if (name != null) "$name=$rendered" else rendered
            }

            override fun visitEnum(name: String?, descriptor: String, value: String) {
                // Do not render enum class name to reduce verbosity.
                arguments +=
                    if (name != null) "$name=$value" else value
            }

            override fun visitAnnotation(name: String?, descriptor: String): AnnotationVisitor? =
                visitAnnotationImpl { args ->
                    val rendered = renderAnnotation(descriptor, args)
                    arguments +=
                        if (name != null) "$name=$rendered" else rendered
                }

            override fun visitArray(name: String): AnnotationVisitor? =
                visitAnnotationImpl { args ->
                    arguments += "$name=[${args.joinToString(", ")}]"
                }

            override fun visitEnd() {
                end(arguments)
            }
        }

    private fun renderAnnotation(desc: String, args: List<String>): String {
        val name = Type.getType(desc).className
        return if (args.isEmpty() || desc == "Lkotlin/Metadata;" || desc == "Lkotlin/coroutines/jvm/internal/DebugMetadata;")
            name
        else
            "$name(${args.joinToString(", ")})"
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
        classAccess = access
        classSignature = signature
    }

    override fun visitOuterClass(owner: String, name: String?, descriptor: String?) {
        if (name == null) {
            KtAssert.assertNull("", descriptor)
            declarationsInsideClass.add(Declaration("enclosing class $owner"))
        } else {
            KtAssert.assertNotNull("", descriptor)
            declarationsInsideClass.add(Declaration("enclosing method $owner.$name$descriptor"))
        }
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (!filter.shouldWriteInnerClass(name, outerName, innerName)) {
            return
        }

        when {
            innerName == null -> {
                KtAssert.assertNull(
                    "Anonymous classes should have neither innerName nor outerName. Name=$name, outerName=$outerName",
                    outerName
                )
                declarationsInsideClass.add(Declaration("inner (anonymous) class $name"))
            }
            outerName == null -> {
                declarationsInsideClass.add(Declaration("inner (local) class $name $innerName"))
            }
            name == "$outerName$$innerName" -> {
                declarationsInsideClass.add(Declaration("${getModifiers(ModifierTarget.CLASS, access)} inner class $name"))
            }
            else -> {
                declarationsInsideClass.add(Declaration("inner (unrecognized) class $name $outerName $innerName"))
            }
        }
    }
}
