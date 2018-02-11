/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File

abstract class AbstractBytecodeListingTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        compile(files, javaFilesDir)
        val actualTxt = BytecodeListingTextCollectingVisitor.getText(classFileFactory, withSignatures = isWithSignatures(wholeFile))
        KotlinTestUtils.assertEqualsToFile(txtFile, actualTxt)
    }

    private fun isWithSignatures(wholeFile: File): Boolean =
            WITH_SIGNATURES.containsMatchIn(wholeFile.readText())

    companion object {
        private val WITH_SIGNATURES = Regex.fromLiteral("// WITH_SIGNATURES")
    }
}

class BytecodeListingTextCollectingVisitor(val filter: Filter, val withSignatures: Boolean) : ClassVisitor(ASM5) {
    companion object {
        @JvmOverloads
        fun getText(
                factory: ClassFileFactory,
                filter: Filter = Filter.EMPTY,
                replaceHash: Boolean = true,
                withSignatures: Boolean = false
        ) =
                factory.getClassFiles()
                        .sortedBy { it.relativePath }
                        .mapNotNull {
                            val cr = ClassReader(it.asByteArray())
                            val visitor = BytecodeListingTextCollectingVisitor(filter, withSignatures)
                            cr.accept(visitor, ClassReader.SKIP_CODE)

                            if (!filter.shouldWriteClass(cr.access, cr.className)) {
                                return@mapNotNull null
                            }

                            if (replaceHash) {
                                KotlinTestUtils.replaceHash(visitor.text, "HASH")
                            }
                            else {
                                visitor.text
                            }
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

        return object : MethodVisitor(ASM5) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                val type = Type.getType(desc).className
                methodAnnotations += "@$type "
                return super.visitAnnotation(desc, visible)
            }

            override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? {
                val type = Type.getType(desc).className
                parameterAnnotations.getOrPut(parameter, { arrayListOf() }).add("@$type ")
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

        return object : FieldVisitor(ASM5) {
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
