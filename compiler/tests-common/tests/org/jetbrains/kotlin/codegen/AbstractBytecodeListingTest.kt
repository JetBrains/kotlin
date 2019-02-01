/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File

abstract class AbstractBytecodeListingTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {

        compile(files, javaFilesDir)
        val actualTxt = BytecodeListingTextCollectingVisitor.getText(classFileFactory, withSignatures = isWithSignatures(wholeFile))

        val prefixes =
            if (coroutinesPackage == DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_RELEASE.asString()) {
                listOf("_1_3", "")
            } else listOf("")

        val txtFile =
            prefixes.firstNotNullResult { File(wholeFile.parentFile, wholeFile.nameWithoutExtension + "$it.txt").takeIf(File::exists) }
                .sure { "No testData file exists: ${wholeFile.nameWithoutExtension}.txt" }

        KotlinTestUtils.assertEqualsToFile(txtFile, actualTxt) {
            it.replace("COROUTINES_PACKAGE", coroutinesPackage)
        }
    }

    private fun isWithSignatures(wholeFile: File): Boolean =
            WITH_SIGNATURES.containsMatchIn(wholeFile.readText())

    companion object {
        private val WITH_SIGNATURES = Regex.fromLiteral("// WITH_SIGNATURES")
    }
}

class BytecodeListingTextCollectingVisitor(val filter: Filter, val withSignatures: Boolean, api: Int = API_VERSION) : ClassVisitor(api) {
    companion object {
        @JvmOverloads
        fun getText(
            factory: ClassFileFactory,
            filter: Filter = Filter.EMPTY,
            withSignatures: Boolean = false
        ) = factory.getClassFiles()
            .sortedBy { it.relativePath }
            .mapNotNull {
                val cr = ClassReader(it.asByteArray())
                val visitor = BytecodeListingTextCollectingVisitor(filter, withSignatures)
                cr.accept(visitor, ClassReader.SKIP_CODE)

                if (!filter.shouldWriteClass(cr.access, cr.className)) null else visitor.text
            }.joinToString("\n\n", postfix = "\n")
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

    private fun handleModifiers(access: Int, list: MutableList<String> = declarationsInsideClass.last().annotations) {
        if (access and ACC_PUBLIC != 0) addModifier("public", list)
        if (access and ACC_PROTECTED != 0) addModifier("protected", list)
        if (access and ACC_PRIVATE != 0) addModifier("private", list)

        if (access and ACC_SYNTHETIC != 0) addModifier("synthetic", list)
        if (access and ACC_DEPRECATED != 0) addModifier("deprecated", list)
        if (access and ACC_FINAL != 0) addModifier("final", list)
        if (access and ACC_ABSTRACT != 0 && access and ACC_INTERFACE == 0) addModifier("abstract", list)
        if (access and ACC_STATIC != 0) addModifier("static", list)
    }

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
            arrayListOf<String>().apply { handleModifiers(classAccess, this) }.forEach { append(it) }
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

    override fun visitMethod(
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?
    ): MethodVisitor? {
        if (!filter.shouldWriteMethod(access, name, desc)) {
            return null
        }

        val returnType = Type.getReturnType(desc).className
        val parameterTypes = Type.getArgumentTypes(desc).map { it.className }
        val methodAnnotations = arrayListOf<String>()
        val parameterAnnotations = hashMapOf<Int, MutableList<String>>()

        handleModifiers(access, methodAnnotations)
        val methodParamCount = Type.getArgumentTypes(desc).size

        return object : MethodVisitor(API_VERSION) {
            private var visibleAnnotableParameterCount = methodParamCount
            private var invisibleAnnotableParameterCount = methodParamCount

            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                val type = Type.getType(desc).className
                methodAnnotations += "@$type "
                return super.visitAnnotation(desc, visible)
            }

            override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
                val type = Type.getType(desc).className
                parameterAnnotations.getOrPut(
                    parameter + methodParamCount - (if (visible) visibleAnnotableParameterCount else invisibleAnnotableParameterCount),
                    { arrayListOf() }).add("@$type ")
                return super.visitParameterAnnotation(parameter, desc, visible)
            }

            override fun visitEnd() {
                val parameterWithAnnotations = parameterTypes.mapIndexed { index, parameter ->
                    val annotations = parameterAnnotations.getOrElse(index, { emptyList<String>() }).joinToString("")
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
        handleModifiers(access)
        if (access and ACC_VOLATILE != 0) addModifier("volatile", fieldDeclaration.annotations)

        return object : FieldVisitor(API_VERSION) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                addAnnotation(desc)
                return super.visitAnnotation(desc, visible)
            }
        }
    }

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
        val name = Type.getType(desc).className
        classAnnotations.add("@$name")
        return super.visitAnnotation(desc, visible)
    }

    override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
    ) {
        className = name
        classAccess = access
        classSignature = signature
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        if (!filter.shouldWriteInnerClass(name)) {
            return
        }
        declarationsInsideClass.add(Declaration("inner class $name"))
    }
}
