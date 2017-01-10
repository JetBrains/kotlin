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

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.jetbrains.org.objectweb.asm.Opcodes.INVOKESTATIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.util.Printer

class CallableMethod(
        override val owner: Type,
        private val defaultImplOwner: Type?,
        private val defaultMethodDesc: String,
        private val signature: JvmMethodSignature,
        private val invokeOpcode: Int,
        override val dispatchReceiverType: Type?,
        override val extensionReceiverType: Type?,
        override val generateCalleeType: Type?,
        private val isInterfaceMethod: Boolean = Opcodes.INVOKEINTERFACE == invokeOpcode
) : Callable {
    fun getValueParameters(): List<JvmMethodParameterSignature> =
            signature.valueParameters

    override val valueParameterTypes: List<Type>
        get() = signature.valueParameters.filter { it.kind == JvmMethodParameterKind.VALUE }.map { it.asmType }

    fun getAsmMethod(): Method =
            signature.asmMethod

    override val parameterTypes: Array<Type>
        get() = getAsmMethod().argumentTypes


    override fun genInvokeInstruction(v: InstructionAdapter) {
        v.visitMethodInsn(
                invokeOpcode,
                owner.internalName,
                getAsmMethod().name,
                getAsmMethod().descriptor,
                isInterfaceMethod
        )
    }

    fun genInvokeDefaultInstruction(v: InstructionAdapter) {
        if (defaultImplOwner == null) {
            throw IllegalStateException()
        }

        val method = getAsmMethod()

        if ("<init>" == method.name) {
            v.visitMethodInsn(INVOKESPECIAL, defaultImplOwner.internalName, "<init>", defaultMethodDesc, false)
        }
        else {
            v.visitMethodInsn(INVOKESTATIC, defaultImplOwner.internalName,
                              method.name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, defaultMethodDesc, false)

            StackValue.coerce(Type.getReturnType(defaultMethodDesc), Type.getReturnType(signature.asmMethod.descriptor), v)
        }
    }

    override val returnType: Type
        get() = signature.returnType

    override fun isStaticCall(): Boolean =
            invokeOpcode == INVOKESTATIC

    override fun toString(): String =
            "${Printer.OPCODES[invokeOpcode]} $owner.$signature"
}
