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
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*

public abstract class AbstractBytecodeListingTest : CodegenTestCase() {

    throws(Exception::class)
    public fun doTest(filename: String) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL)
        loadFileByFullPath(filename)
        val ktFile = File(filename)
        val txtFile = File(ktFile.parent, ktFile.nameWithoutExtension + ".txt")
        val generatedFiles = CodegenTestUtil.generateFiles(myEnvironment, myFiles)
                .asList()
                .sortedBy { it.relativePath }
                .map {
                    val cr = ClassReader(it.asByteArray())
                    val visitor = TextCollectingVisitor()
                    cr.accept(visitor, ClassReader.SKIP_CODE)
                    visitor.text
                }.joinToString("\n\n")

        JetTestUtils.assertEqualsToFile(txtFile, generatedFiles)
    }

    private class TextCollectingVisitor : ClassVisitor(Opcodes.ASM5) {
        private class Declaration(val text: String, val annotations: MutableList<String> = arrayListOf())

        private val declarationsInsideClass = arrayListOf<Declaration>()
        private val classAnnotations = arrayListOf<String>()
        private var className = ""

        private fun addAnnotation(desc: String) {
            val name = Type.getType(desc).className
            declarationsInsideClass.last().annotations.add("@$name ")
        }

        public val text: String
            get() = StringBuilder {
                append(classAnnotations.joinToString(""))
                append(className)
                if (declarationsInsideClass.isNotEmpty()) {
                    append(" {\n")
                    for (declaration in declarationsInsideClass) {
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

            return object : MethodVisitor(Opcodes.ASM5) {
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
            return object : FieldVisitor(Opcodes.ASM5) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    addAnnotation(desc)
                    return super.visitAnnotation(desc, visible)
                }
            }
        }

        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            val name = Type.getType(desc).className
            classAnnotations.add("@$name ")
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
        }

        override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
            declarationsInsideClass.add(Declaration("inner class $name"))
        }
    }

}
