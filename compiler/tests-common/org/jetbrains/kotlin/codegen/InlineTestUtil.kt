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

import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.inline.inlineFunctionsJvmNames
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*

object InlineTestUtil {
    fun checkNoCallsToInline(files: Iterable<OutputFile>, sourceFiles: List<KtFile>) {
        val inlineInfo = obtainInlineInfo(files)
        val inlineMethods = inlineInfo.inlineMethods
        assert(!inlineMethods.isEmpty()) { "There are no inline methods" }

        val notInlinedCalls = checkInlineMethodNotInvoked(files, inlineMethods)
        assert(notInlinedCalls.isEmpty()) { "All inline methods should be inlined but:\n" + notInlinedCalls.joinToString("\n") }

        val skipParameterChecking = sourceFiles.any {
            InTextDirectivesUtils.isDirectiveDefined(it.text, "NO_CHECK_LAMBDA_INLINING")
        }

        if (!skipParameterChecking) {
            val notInlinedParameters = checkParametersInlined(files, inlineInfo)
            assert(notInlinedParameters.isEmpty()) {
                "All inline parameters should be inlined but:\n${notInlinedParameters.joinToString("\n")}\n" +
                "but if you have not inlined lambdas or anonymous objects enable NO_CHECK_LAMBDA_INLINING directive"
            }
        }
    }

    private fun obtainInlineInfo(files: Iterable<OutputFile>): InlineInfo {
        val inlineMethods = HashSet<MethodInfo>()
        val binaryClasses = hashMapOf<String, KotlinJvmBinaryClass>()

        for (file in files) {
            val bytes = file.asByteArray()
            val cr = ClassReader(bytes)

            val inlineFunctions = inlineFunctionsJvmNames(bytes)

            val classVisitor = object : ClassVisitorWithName() {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
                    return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                        override fun visitEnd() {
                            if (name + desc in inlineFunctions) {
                                inlineMethods.add(MethodInfo(className, name, this.desc))
                            }
                        }
                    }
                }
            }

            cr.accept(classVisitor, 0)
            binaryClasses.put(classVisitor.className, loadBinaryClass(file))
        }

        return InlineInfo(inlineMethods, binaryClasses)
    }

    private fun checkInlineMethodNotInvoked(files: Iterable<OutputFile>, inlinedMethods: Set<MethodInfo>): List<NotInlinedCall> {
        val notInlined = ArrayList<NotInlinedCall>()

        files.forEach { file ->
            ClassReader(file.asByteArray()).accept(object : ClassVisitorWithName() {
                private var skipMethodsOfThisClass = false

                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    if (desc == JvmAnnotationNames.METADATA_DESC) {
                        return object : AnnotationVisitor(Opcodes.ASM5) {
                            override fun visit(name: String?, value: Any) {
                                if (name == JvmAnnotationNames.KIND_FIELD_NAME && value == KotlinClassHeader.Kind.MULTIFILE_CLASS.id) {
                                    skipMethodsOfThisClass = true
                                }
                            }
                        }
                    }

                    return null
                }

                override fun visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array<String>): MethodVisitor? {
                    if (skipMethodsOfThisClass) {
                        return null
                    }

                    return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            val methodCall = MethodInfo(owner, name, desc)
                            if (inlinedMethods.contains(methodCall)) {
                                val fromCall = MethodInfo(className, this.name, this.desc)

                                //skip delegation to interface DefaultImpls from child class
                                if (methodCall.owner.endsWith(JvmAbi.DEFAULT_IMPLS_SUFFIX) && fromCall.owner != methodCall.owner) {
                                    return
                                }
                                notInlined.add(NotInlinedCall(fromCall, methodCall))
                            }
                        }
                    }
                }
            }, 0)
        }

        return notInlined
    }

    private fun checkParametersInlined(files: Iterable<OutputFile>, inlineInfo: InlineInfo): ArrayList<NotInlinedParameter> {
        val inlinedMethods = inlineInfo.inlineMethods
        val notInlinedParameters = ArrayList<NotInlinedParameter>()
        for (file in files) {
            if (!isClassOrPackagePartKind(loadBinaryClass(file))) continue

            ClassReader(file.asByteArray()).accept(object : ClassVisitorWithName() {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
                    val declaration = MethodInfo(className, name, desc)
                    //do not check anonymous object creation in inline functions and in package facades
                    if (declaration in inlinedMethods) {
                        return null
                    }

                    return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                        private fun isInlineParameterLikeOwner(owner: String) =
                                "$" in owner && !isTopLevelOrInnerOrPackageClass(owner, inlineInfo)

                        override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            if ("<init>".equals(name) && isInlineParameterLikeOwner(owner)) {
                                val fromCall = MethodInfo(className, this.name, this.desc)
                                notInlinedParameters.add(NotInlinedParameter(owner, fromCall))
                            }
                        }

                        override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
                            if (opcode == Opcodes.GETSTATIC && isInlineParameterLikeOwner(owner)) {
                                val fromCall = MethodInfo(className, this.name, this.desc)
                                notInlinedParameters.add(NotInlinedParameter(owner, fromCall))
                            }
                            super.visitFieldInsn(opcode, owner, name, desc)
                        }
                    }
                }
            }, 0)
        }

        return notInlinedParameters
    }

    private fun isTopLevelOrInnerOrPackageClass(classInternalName: String, inlineInfo: InlineInfo): Boolean {
        if (classInternalName.startsWith("kotlin/jvm/internal/"))
            return true

        return isClassOrPackagePartKind(inlineInfo.binaryClasses[classInternalName]!!)
    }

    private fun isClassOrPackagePartKind(klass: KotlinJvmBinaryClass): Boolean {
        return klass.classHeader.kind == KotlinClassHeader.Kind.CLASS && !klass.classId.isLocal
    }

    private fun loadBinaryClass(file: OutputFile): KotlinJvmBinaryClass {
        val klass = FileBasedKotlinClass.create(file.asByteArray()) {
            className, classHeader, innerClasses ->
            object : FileBasedKotlinClass(className, classHeader, innerClasses) {
                override val location: String
                    get() = throw UnsupportedOperationException()
                override fun getFileContents(): ByteArray = throw UnsupportedOperationException()
                override fun hashCode(): Int = throw UnsupportedOperationException()
                override fun equals(other: Any?): Boolean = throw UnsupportedOperationException()
                override fun toString(): String = throw UnsupportedOperationException()
            }
        }!!
        return klass
    }

    private class InlineInfo(val inlineMethods: Set<MethodInfo>, val binaryClasses: Map<String, KotlinJvmBinaryClass>)

    private data class NotInlinedCall(val fromCall: MethodInfo, val inlineMethod: MethodInfo)

    private data class NotInlinedParameter(val parameterClassName: String, val fromCall: MethodInfo)

    private data class MethodInfo(val owner: String, val name: String, val desc: String)

    private open class ClassVisitorWithName : ClassVisitor(Opcodes.ASM5) {
        lateinit var className: String

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            className = name
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }
}
