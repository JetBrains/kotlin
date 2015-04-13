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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.utils.join
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.ArrayList
import java.util.HashSet
import kotlin.properties.Delegates

public object InlineTestUtil {

    public val INLINE_ANNOTATION_CLASS: String = "kotlin/inline"

    public fun checkNoCallsToInline(files: List<OutputFile>, sourceFiles: List<JetFile>) {
        val inlineInfo = obtainInlineInfo(files)
        val inlineMethods = inlineInfo.inlineMethods
        assert(!inlineMethods.isEmpty(), "There are no inline methods")

        val notInlinedCalls = checkInlineMethodNotInvoked(files, inlineMethods)
        assert(notInlinedCalls.isEmpty()) { "All inline methods should be inlined but:\n" + notInlinedCalls.joinToString("\n") }


        val skipParameterChecking =
                sourceFiles.sequence().filter {
                    InTextDirectivesUtils.isDirectiveDefined(it.getText(), "NO_CHECK_LAMBDA_INLINING")
                }.any()

        if (!skipParameterChecking) {
            val notInlinedParameters = checkParametersInlined(files, inlineInfo)
            assert(notInlinedParameters.isEmpty()) { "All inline parameters should be inlined but:\n${notInlinedParameters.joinToString("\n")}\n" +
                                                     "but if you have not inlined lambdas or anonymous objects enable NO_CHECK_LAMBDA_INLINING directive" }
        }
    }

    private fun obtainInlineInfo(files: List<OutputFile>): InlineInfo {
        val inlineMethods = HashSet<MethodInfo>()
        val classHeaders = hashMapOf<String, KotlinClassHeader>()

        for (file in files) {
            val cr = ClassReader(file.asByteArray())

            val classVisitor = object : ClassVisitorWithName() {

                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
                    return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                        public override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
                            val type = Type.getType(desc)
                            val annotationClass = type.getInternalName()
                            if (INLINE_ANNOTATION_CLASS == annotationClass) {
                                inlineMethods.add(MethodInfo(className, name, this.desc))
                            }
                            return super.visitAnnotation(desc, visible)
                        }
                    }
                }
            }
            cr.accept(classVisitor, 0)

            classHeaders.put(classVisitor.className, getClassHeader(file))
        }
        return InlineInfo(inlineMethods, classHeaders)
    }

    private fun checkInlineMethodNotInvoked(files: List<OutputFile>, inlinedMethods: Set<MethodInfo>): List<NotInlinedCall> {
        val notInlined = ArrayList<NotInlinedCall>()

        files.forEach { file ->

            val cr = ClassReader(file.asByteArray())
            cr.accept(object : ClassVisitorWithName() {

                override fun visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array<String>): MethodVisitor? {
                    val classFqName = JvmClassName.byInternalName(className).getFqNameForClassNameWithoutDollars()
                    if (PackageClassUtils.isPackageClassFqName(classFqName)) {
                        return null
                    }

                    return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                        public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            val methodCall = MethodInfo(owner, name, desc)
                            if (inlinedMethods.contains(methodCall)) {
                                val fromCall = MethodInfo(className, this.name, this.desc)

                                //skip delegation to trait impl from child class
                                if (methodCall.owner.endsWith(JvmAbi.TRAIT_IMPL_SUFFIX) && fromCall.owner != methodCall.owner) {
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

    private fun checkParametersInlined(files: List<OutputFile>, inlineInfo: InlineInfo): ArrayList<NotInlinedParameter> {
        val inlinedMethods = inlineInfo.inlineMethods
        val notInlinedParameters = ArrayList<NotInlinedParameter>()
        for (file in files) {
            val kotlinClassHeader = getClassHeader(file)
            if (isClassOrPackagePartKind(kotlinClassHeader)) {
                val cr = ClassReader(file.asByteArray())

                cr.accept(object : ClassVisitorWithName() {

                    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor? {
                        val classFqName = JvmClassName.byInternalName(className).getFqNameForClassNameWithoutDollars()
                        val declaration = MethodInfo(className, name, desc)
                        //do not check anonymous object creation in inline functions and in package facades
                        if (PackageClassUtils.isPackageClassFqName(classFqName) || declaration in inlinedMethods) {
                            return null
                        }

                        return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                            private fun isInlineParameterLikeOwner(owner: String) = owner.contains("$") && !isTopLevelOrInnerOrPackageClass(owner, inlineInfo)

                            public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                                if ("<init>".equals(name) && isInlineParameterLikeOwner(owner)) {
                                    /*constuctor creation*/
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
        }

        return notInlinedParameters
    }

    private fun isTopLevelOrInnerOrPackageClass(classInternalName: String, inlineInfo: InlineInfo): Boolean {
        if (classInternalName.startsWith("kotlin/jvm/internal/"))
            return true

        return isClassOrPackagePartKind(inlineInfo.classHeaders[classInternalName]!!)
    }

    private fun isClassOrPackagePartKind(header: KotlinClassHeader): Boolean {
        return header.classKind == JvmAnnotationNames.KotlinClass.Kind.CLASS
               || header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.PACKAGE_PART
               || header.syntheticClassKind == JvmAnnotationNames.KotlinSyntheticClass.Kind.TRAIT_IMPL
    }

    private fun getClassHeader(file: OutputFile): KotlinClassHeader {
        return FileBasedKotlinClass.create(file.asByteArray()) {
            className, classHeader, innerClasses ->
            object : FileBasedKotlinClass(className, classHeader, innerClasses) {
                override fun getLocation(): String = throw UnsupportedOperationException()
                override fun getFileContents(): ByteArray = throw UnsupportedOperationException()
                override fun hashCode(): Int = throw UnsupportedOperationException()
                override fun equals(other: Any?): Boolean = throw UnsupportedOperationException()
                override fun toString(): String? = throw UnsupportedOperationException()
            }
        }.getClassHeader()
    }

    private class InlineInfo(val inlineMethods: Set<MethodInfo>, val classHeaders: Map<String, KotlinClassHeader>)

    private data class NotInlinedCall(val fromCall: MethodInfo, val inlineMethod: MethodInfo)

    private data class NotInlinedParameter(val parameterClassName: String, val fromCall: MethodInfo)

    private data class MethodInfo(val owner: String, val name: String, val desc: String)

    open private class ClassVisitorWithName() : ClassVisitor(Opcodes.ASM5) {

        var className: String by Delegates.notNull()

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            className = name
            super.visit(version, access, name, signature, superName, interfaces)
        }
    }
}
