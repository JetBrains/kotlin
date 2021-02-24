/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.jetbrains.org.objectweb.asm.Opcodes.INVOKESTATIC
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.util.Printer

class CallableMethod(
    override val owner: Type,
    private val defaultImplOwner: Type?,
    computeDefaultMethod: () -> Method,
    private val signature: JvmMethodSignature,
    val invokeOpcode: Int,
    override val dispatchReceiverType: Type?,
    override val dispatchReceiverKotlinType: KotlinType?,
    override val extensionReceiverType: Type?,
    override val extensionReceiverKotlinType: KotlinType?,
    override val generateCalleeType: Type?,
    override val returnKotlinType: KotlinType?,
    val isInterfaceMethod: Boolean,
    private val isDefaultMethodInInterface: Boolean,
    private val boxInlineClassBeforeInvoke: Boolean
) : Callable {
    private val defaultImplMethod: Method by lazy(LazyThreadSafetyMode.PUBLICATION, computeDefaultMethod)

    private val defaultImplMethodName: String get() = defaultImplMethod.name
    private val defaultMethodDesc: String get() = defaultImplMethod.descriptor

    fun getValueParameters(): List<JvmMethodParameterSignature> =
        signature.valueParameters

    override val valueParameterTypes: List<Type>
        get() = signature.valueParameters.filter { it.kind == JvmMethodParameterKind.VALUE }.map { it.asmType }

    fun getAsmMethod(): Method =
        signature.asmMethod

    override val parameterTypes: Array<Type>
        get() = getAsmMethod().argumentTypes

    override fun genInvokeInstruction(v: InstructionAdapter) {
        if (boxInlineClassBeforeInvoke) {
            StackValue.boxInlineClass(dispatchReceiverKotlinType!!, v)
        }
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
        } else {
            v.visitMethodInsn(
                INVOKESTATIC, defaultImplOwner.internalName,
                defaultImplMethodName, defaultMethodDesc, isDefaultMethodInInterface
            )

            StackValue.coerce(
                Type.getReturnType(defaultMethodDesc),
                Type.getReturnType(signature.asmMethod.descriptor),
                v
            )
        }
    }

    override val returnType: Type
        get() = signature.returnType

    override fun isStaticCall(): Boolean =
        invokeOpcode == INVOKESTATIC

    override fun toString(): String =
        "${Printer.OPCODES[invokeOpcode]} $owner.$signature"
}
