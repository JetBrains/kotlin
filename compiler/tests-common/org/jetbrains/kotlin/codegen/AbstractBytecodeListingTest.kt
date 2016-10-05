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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.io.File

abstract class AbstractBytecodeListingTest : CodegenTestCase() {
    protected open val classBuilderFactory: ClassBuilderFactory
        get() = ClassBuilderFactories.TEST

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val javaSources = javaFilesDir?.let { arrayOf(it) } ?: emptyArray()

        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL, *javaSources)
        loadMultiFiles(files)

        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        val generatedFiles = CodegenTestUtil.generateFiles(myEnvironment, myFiles, classBuilderFactory)
                .getClassFiles()
                .sortedBy { it.relativePath }
                .map {
                    val cr = ClassReader(it.asByteArray())
                    val visitor = TextCollectingVisitor()
                    cr.accept(visitor, ClassReader.SKIP_CODE)
                    KotlinTestUtils.replaceHash(visitor.text, "HASH")
                }.joinToString("\n\n")

        KotlinTestUtils.assertEqualsToFile(txtFile, generatedFiles)
    }

    private class TextCollectingVisitor : ClassVisitor(ASM5) {
        private class Declaration(val text: String, val annotations: MutableList<String> = arrayListOf())

        private val declarationsInsideClass = arrayListOf<Declaration>()
        private val classAnnotations = arrayListOf<String>()
        private var className = ""
        private var classAccess = 0

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
                append(classAnnotations.joinToString("\n", postfix = "\n"))
                arrayListOf<String>().apply { handleModifiers(classAccess, this) }.forEach { append(it) }
                append(classOrInterface(classAccess))
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
                    declarationsInsideClass.add(Declaration("method $name($parameterWithAnnotations): $returnType", methodAnnotations))
                    super.visitEnd()
                }
            }
        }

        override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
            val type = Type.getType(desc).className
            declarationsInsideClass.add(Declaration("field $name: $type"))
            handleModifiers(access)

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
        }

        override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
            declarationsInsideClass.add(Declaration("inner class $name"))
        }
    }

}
