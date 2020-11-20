/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class AbstractBytecodeListingTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compile(files)
        val actualTxt = BytecodeListingTextCollectingVisitor.getText(
            classFileFactory,
            withSignatures = isWithSignatures(wholeFile),
            withAnnotations = isWithAnnotations(wholeFile),
            filter = object : BytecodeListingTextCollectingVisitor.Filter {
                override fun shouldWriteClass(access: Int, name: String): Boolean = !name.startsWith("helpers/")
                override fun shouldWriteMethod(access: Int, name: String, desc: String): Boolean = true
                override fun shouldWriteField(access: Int, name: String, desc: String): Boolean = true
                override fun shouldWriteInnerClass(name: String): Boolean = true
            }
        )

        val prefixes = when {
            backend.isIR -> listOf("_ir", "_1_3", "")
            coroutinesPackage == StandardNames.COROUTINES_PACKAGE_FQ_NAME_RELEASE.asString() -> listOf("_1_3", "")
            else -> listOf("")
        }

        val txtFile =
            prefixes.firstNotNullResult { File(wholeFile.parentFile, wholeFile.nameWithoutExtension + "$it.txt").takeIf(File::exists) }
                .sure { "No testData file exists: ${wholeFile.nameWithoutExtension}.txt" }

        KotlinTestUtils.assertEqualsToFile(txtFile, actualTxt)

        if (backend.isIR) {
            val jvmGoldenFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
            val jvmIrGoldenFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + "_ir.txt")
            if (jvmGoldenFile.exists() && jvmIrGoldenFile.exists()) {
                if (jvmGoldenFile.readText() == jvmIrGoldenFile.readText()) {
                    fail("JVM and JVM_IR golden files are identical. Remove $jvmIrGoldenFile.")
                }
            }
        }
    }

    private fun isWithSignatures(wholeFile: File): Boolean =
        WITH_SIGNATURES.containsMatchIn(wholeFile.readText())

    private fun isWithAnnotations(wholeFile: File): Boolean =
        !IGNORE_ANNOTATIONS.containsMatchIn(wholeFile.readText())

    companion object {
        private val WITH_SIGNATURES = Regex.fromLiteral("// WITH_SIGNATURES")
        private val IGNORE_ANNOTATIONS = Regex.fromLiteral("// IGNORE_ANNOTATIONS")
    }
}

class BytecodeListingTextCollectingVisitor(
    val filter: Filter,
    val withSignatures: Boolean,
    api: Int = API_VERSION,
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
                Modifier("public", ACC_PUBLIC, CLASS_OR_FIELD_OR_METHOD),
                Modifier("protected", ACC_PROTECTED, CLASS_OR_FIELD_OR_METHOD),
                Modifier("private", ACC_PRIVATE, CLASS_OR_FIELD_OR_METHOD),
                Modifier("synthetic", ACC_SYNTHETIC, CLASS_OR_FIELD_OR_METHOD),
                Modifier("bridge", ACC_BRIDGE, METHOD_ONLY),
                Modifier("volatile", ACC_VOLATILE, FIELD_ONLY),
                Modifier("synchronized", ACC_SYNCHRONIZED, METHOD_ONLY),
                Modifier("varargs", ACC_VARARGS, METHOD_ONLY),
                Modifier("transient", ACC_TRANSIENT, FIELD_ONLY),
                Modifier("native", ACC_NATIVE, METHOD_ONLY),
                Modifier("deprecated", ACC_DEPRECATED, CLASS_OR_FIELD_OR_METHOD),
                Modifier("final", ACC_FINAL, CLASS_OR_FIELD_OR_METHOD),
                Modifier("strict", ACC_STRICT, METHOD_ONLY),
                Modifier("enum", ACC_ENUM, FIELD_ONLY), // ACC_ENUM modifier on class is handled in 'classOrInterface'
                Modifier("abstract", ACC_ABSTRACT, CLASS_OR_METHOD, excludedMask = ACC_INTERFACE),
                Modifier("static", ACC_STATIC, FIELD_OR_METHOD)
            )
    }

    interface Filter {
        fun shouldWriteClass(access: Int, name: String): Boolean
        fun shouldWriteMethod(access: Int, name: String, desc: String): Boolean
        fun shouldWriteField(access: Int, name: String, desc: String): Boolean
        fun shouldWriteInnerClass(name: String): Boolean

        object EMPTY : Filter {
            override fun shouldWriteClass(access: Int, name: String) = true
            override fun shouldWriteMethod(access: Int, name: String, desc: String) = true
            override fun shouldWriteField(access: Int, name: String, desc: String) = true
            override fun shouldWriteInnerClass(name: String) = true
        }
    }

    private class Declaration(val text: String, val annotations: MutableList<String> = arrayListOf())

    private val declarationsInsideClass = arrayListOf<Declaration>()
    private val classAnnotations = arrayListOf<String>()
    private var className = ""
    private var classAccess = 0
    private var classSignature: String? = ""

    private fun addAnnotation(desc: String, list: MutableList<String> = declarationsInsideClass.last().annotations) {
        val name = Type.getType(desc).className
        list.add("@$name ")
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
            access and ACC_ANNOTATION != 0 -> "annotation class"
            access and ACC_ENUM != 0 -> "enum class"
            access and ACC_INTERFACE != 0 -> "interface"
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

        return object : MethodVisitor(API_VERSION) {
            private var visibleAnnotableParameterCount = methodParamCount
            private var invisibleAnnotableParameterCount = methodParamCount

            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (withAnnotations) {
                    val type = Type.getType(desc).className
                    methodAnnotations += "@$type "
                }
                return super.visitAnnotation(desc, visible)
            }

            override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
                if (withAnnotations) {
                    val type = Type.getType(desc).className
                    parameterAnnotations.getOrPut(
                        parameter + methodParamCount - (if (visible) visibleAnnotableParameterCount else invisibleAnnotableParameterCount),
                        { arrayListOf() }).add("@$type ")
                }
                return super.visitParameterAnnotation(parameter, desc, visible)
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
        if (access and ACC_VOLATILE != 0) addModifier("volatile", fieldDeclaration.annotations)

        return object : FieldVisitor(API_VERSION) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (withAnnotations) {
                    addAnnotation(desc)
                }
                return super.visitAnnotation(desc, visible)
            }
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        if (withAnnotations) {
            val name = Type.getType(desc).className
            classAnnotations.add("@$name")
        }
        return super.visitAnnotation(desc, visible)
    }

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
        classAccess = access
        classSignature = signature
    }

    override fun visitOuterClass(owner: String, name: String?, descriptor: String?) {
        if (name == null) {
            assertNull(descriptor)
            declarationsInsideClass.add(Declaration("enclosing class $owner"))
        } else {
            assertNotNull(descriptor)
            declarationsInsideClass.add(Declaration("enclosing method $owner.$name$descriptor"))
        }
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (!filter.shouldWriteInnerClass(name)) {
            return
        }

        when {
            innerName == null -> {
                assertNull(outerName, "Anonymous classes should have neither innerName nor outerName. Name=$name, outerName=$outerName")
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
