/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor

import java.util.ArrayList

import org.jetbrains.kotlin.codegen.inline.nodeText
import org.jetbrains.kotlin.codegen.inline.wrapWithMaxLocalCalc

abstract class TransformationMethodVisitor(
    private val delegate: MethodVisitor,
    access: Int,
    name: String,
    desc: String,
    signature: String?,
    exceptions: Array<out String>?,
    api: Int = Opcodes.API_VERSION
) : MethodVisitor(api) {

    private val methodNode = MethodNode(access, name, desc, signature, exceptions).apply {
        localVariables = ArrayList(5)
    }

    val traceMethodVisitorIfPossible: TraceMethodVisitor?
        get() {
            val traceMethodVisitor = TraceMethodVisitor(Textifier())
            try {
                methodNode.accept(traceMethodVisitor)
            } catch (e: Throwable) {
                return null
            }

            return traceMethodVisitor
        }

    init {
        mv = wrapWithMaxLocalCalc(methodNode)
    }

    override fun visitEnd() {
        // force mv to calculate maxStack/maxLocals in case it didn't yet done
        if (methodNode.maxLocals <= 0 || methodNode.maxStack <= 0) {
            mv.visitMaxs(-1, -1)
        }

        super.visitEnd()

        try {
            if (shouldBeTransformed(methodNode)) {
                performTransformations(methodNode)
            }

            methodNode.accept(EndIgnoringMethodVisitorDecorator(Opcodes.API_VERSION, delegate))


            // In case of empty instructions list MethodNode.accept doesn't call visitLocalVariables of delegate
            // So we just do it here
            if (methodNode.instructions.size() == 0
                // MethodNode does not create a list of variables for abstract methods, so we would get NPE in accept() instead
                && (delegate !is MethodNode || methodNode.localVariables != null)
            ) {
                val localVariables = methodNode.localVariables
                // visits local variables
                val n = localVariables?.size ?: 0
                for (i in 0 until n) {
                    localVariables!![i].accept(delegate)
                }
            }

            delegate.visitEnd()
        } catch (t: Throwable) {
            throw CompilationException("Couldn't transform method node:\n" + methodNode.nodeText, t, null)
        }
    }

    protected abstract fun performTransformations(methodNode: MethodNode)

    /**
     * You can use it when you need to ignore visit end
     */
    private class EndIgnoringMethodVisitorDecorator(api: Int, mv: MethodVisitor) : MethodVisitor(api, mv) {

        override fun visitEnd() {
        }
    }

    private fun shouldBeTransformed(node: MethodNode): Boolean {
        return node.instructions.size() > 0
    }
}
