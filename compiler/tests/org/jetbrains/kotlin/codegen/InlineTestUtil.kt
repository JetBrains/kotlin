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
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.ArrayList
import java.util.HashSet

public object InlineTestUtil {

    public val INLINE_ANNOTATION_CLASS: String = "kotlin/inline"

    public fun checkNoCallsToInline(files: List<OutputFile>) {
        val inlinedMethods = collectInlineMethods(files)
        assert(!inlinedMethods.isEmpty(), "There are no inline methods")

        val notInlinedCalls = checkInlineNotInvoked(files, inlinedMethods)
        assert(notInlinedCalls.isEmpty()) { "All inline methods should be inlined but " + StringUtil.join(notInlinedCalls, "\n") }
    }

    private fun collectInlineMethods(files: List<OutputFile>): Set<MethodInfo> {
        val inlineMethods = HashSet<MethodInfo>()

        for (file in files) {
            val cr = ClassReader(file.asByteArray())
            var className: String? = null

            cr.accept(object : ClassVisitor(Opcodes.ASM4) {

                override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
                    className = name
                    super.visit(version, access, name, signature, superName, interfaces)
                }

                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
                    return object : MethodNode(Opcodes.ASM4, access, name, desc, signature, exceptions) {
                        public override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
                            val type = Type.getType(desc)
                            val annotationClass = type.getInternalName()
                            if (INLINE_ANNOTATION_CLASS == annotationClass) {
                                inlineMethods.add(MethodInfo(className!!, name, this.desc))
                            }
                            return super.visitAnnotation(desc, visible)
                        }
                    }
                }
            }, 0)

        }
        return inlineMethods
    }

    private fun checkInlineNotInvoked(files: List<OutputFile>, inlinedMethods: Set<MethodInfo>): List<NotInlinedCall> {
        val notInlined = ArrayList<NotInlinedCall>()
        files.forEach { file ->
            val cr = ClassReader(file.asByteArray())
            var className: String? = null
            cr.accept(object : ClassVisitor(Opcodes.ASM4) {
                override fun visit(version: Int, access: Int, name: String, signature: String, superName: String, interfaces: Array<String>) {
                    className = name
                    super.visit(version, access, name, signature, superName, interfaces)
                }

                override fun visitMethod(access: Int, name: String, desc: String, signature: String, exceptions: Array<String>): MethodVisitor? {
                    val classFqName = JvmClassName.byInternalName(className!!).getFqNameForClassNameWithoutDollars()
                    if (PackageClassUtils.isPackageClassFqName(classFqName)) {
                        return null
                    }

                    return object : MethodNode(Opcodes.ASM4, access, name, desc, signature, exceptions) {
                        public override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                            val methodCall = MethodInfo(owner, name, desc)
                            if (inlinedMethods.contains(methodCall)) {
                                val fromCall = MethodInfo(className!!, this.name, this.desc)

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

    private data class NotInlinedCall(public val fromCall: MethodInfo, public val inlineMethod: MethodInfo)

    private data class MethodInfo(val owner: String, val name: String, val desc: String)
}
