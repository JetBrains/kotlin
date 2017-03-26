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

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method

abstract class ProtocolGenerator(protected val codegen: ExpressionCodegen) {
    private val generated = mutableMapOf<String, Boolean>()

    fun putInvokerAndGenerateIfNeeded(method: CallableMethod, call: ResolvedCall<*>) {
        val candidate = call.candidateDescriptor
        val name = "proto$${candidate.containingDeclaration.name}$${candidate.name}$${candidate.valueParameters.joinToString { it.type.toString() }}"

        if (!generated.contains(name)) {
            generated[name] = genProtocolCaller(method, call, name)
        }

        invokeCaller(name, generated[name]!!)
    }

    abstract fun invokeMethod(method: Callable)

    abstract fun invokeCaller(name: String, hasPrimitive: Boolean)

    abstract fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable)

    protected abstract fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String): Boolean

    protected fun putPrimitiveFlags(arguments: List<ValueParameterDescriptor>) {
        codegen.v.iconst(arguments.size)
        codegen.v.newarray(Type.BOOLEAN_TYPE)
        var index = 0
        arguments.forEach {
            val primitive = it.type.isPrimitiveNumberType()
            codegen.v.dup()
            codegen.v.iconst(index)
            index++

            if (primitive)
                codegen.v.iconst(0)
            else
                codegen.v.iconst(1)

            codegen.v.visitInsn(BASTORE)
        }
    }

    protected fun hasPrimitiveArguments(arguments: List<ValueParameterDescriptor>): Boolean {
        if (arguments.any { it.type.isPrimitiveNumberType() }) {
            return true
        }
        return false
    }
}

class IndyProtocolGenerator(codegen: ExpressionCodegen) : ProtocolGenerator(codegen) {

    override fun invokeCaller(name: String, hasPrimitive: Boolean) {
        val signature = Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE)
        codegen.v.invokestatic(codegen.parentCodegen.className, name, signature, false)
    }

    override fun invokeMethod(method: Callable) {
        val args = mutableListOf(OBJECT_TYPE)
        args.addAll(method.valueParameterTypes)

        val descriptor = Type.getMethodDescriptor(method.returnType, *args.toTypedArray())
        codegen.v.invokevirtual("java/lang/invoke/MethodHandle", "invoke", descriptor, false)
    }

    override fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String): Boolean {
        val methodName = call.candidateDescriptor.name

        val descriptor = Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE)
        val mv = codegen.parentCodegen.v.newMethod(JvmDeclarationOrigin.NO_ORIGIN,
                                                   ACC_SYNTHETIC + ACC_STATIC,
                                                   name, descriptor, null, null)

        mv.visitCode()

        val l0 = Label()
        mv.visitLabel(l0)

        val callSite = "kotlin/jvm/internal/ProtocolCallSite"
        val bootstrapDescriptor = Type.getMethodDescriptor(
                Type.getType(CallSite::class.java),
                Type.getType(MethodHandles.Lookup::class.java),
                Type.getType(String::class.java),
                Type.getType(MethodType::class.java),
                Type.getType(String::class.java),
                Type.getType(MethodType::class.java))
        val bootstrap = Handle(H_INVOKESTATIC, callSite, "getBootstrap", bootstrapDescriptor, true)

        mv.visitInvokeDynamicInsn("apply", "()L$callSite;", bootstrap, methodName.asString(), Type.getType(method.getAsmMethod().descriptor))

        mv.visitVarInsn(ALOAD, 0)

        val arguments = call.candidateDescriptor.valueParameters
        val hasPrimitive = hasPrimitiveArguments(arguments)
        var targetDescriptor = descriptor

        if (hasPrimitive) {
            putPrimitiveFlags(arguments)
            targetDescriptor = Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE, Type.getType("[Z"))
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, callSite, "getMethod", targetDescriptor, false)
        mv.visitInsn(ARETURN)

        val l1 = Label()
        mv.visitLabel(l1)
        mv.visitLocalVariable("receiver", OBJECT_TYPE.descriptor, null, l0, l1, 0)
        mv.visitMaxs(0, 0)

        mv.visitEnd()

        return hasPrimitive
    }

    override fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable) {
        val descriptor = resolvedCall.resultingDescriptor
        val valueArguments = resolvedCall.valueArgumentsByIndex ?: error("Failed to arrange value arguments by index: " + descriptor)
        val argGenerator = CallBasedArgumentGenerator(codegen, generator, descriptor.valueParameters, callableMethod.valueParameterTypes)

        argGenerator.generate(valueArguments, resolvedCall.valueArguments.values.toList(), null)
    }
}

class ReflectionProtocolGenerator(codegen: ExpressionCodegen) : ProtocolGenerator(codegen) {

    override fun invokeMethod(method: Callable) {
        codegen.v.invokevirtual("java/lang/reflect/Method", "invoke", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, Type.getType("[Ljava/lang/Object;")), false)
    }

    override fun invokeCaller(name: String, hasPrimitive: Boolean) {
        codegen.v.invokestatic(codegen.parentCodegen.className, name, Type.getMethodDescriptor(Type.getType(Method::class.java), OBJECT_TYPE), false)
    }

    override fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String): Boolean {
        val methodName = call.candidateDescriptor.name
        val descriptor = Type.getMethodDescriptor(Type.getType(Method::class.java), OBJECT_TYPE)

        val mv = codegen.parentCodegen.v.newMethod(JvmDeclarationOrigin.NO_ORIGIN,
                                                   ACC_SYNTHETIC + ACC_STATIC,
                                                   name, descriptor, null, null)
        mv.visitCode()

        val l0 = Label()
        mv.visitLabel(l0)

        val callSite = "kotlin/jvm/internal/ProtocolCallSite"
        val bootstrap = Handle(H_INVOKESTATIC, callSite, "getBootstrap",
                               Type.getMethodDescriptor(
                                       Type.getType(CallSite::class.java),
                                       Type.getType(MethodHandles.Lookup::class.java),
                                       Type.getType(String::class.java),
                                       Type.getType(MethodType::class.java),
                                       Type.getType(String::class.java),
                                       Type.getType(MethodType::class.java)), true)

        mv.visitInvokeDynamicInsn("apply", "()L$callSite;", bootstrap, methodName.asString(), Type.getType(method.getAsmMethod().descriptor))

        mv.visitVarInsn(ALOAD, 0)
        mv.visitMethodInsn(INVOKEVIRTUAL, callSite, "getReflectMethod",
                           Type.getMethodDescriptor(Type.getType(Method::class.java), OBJECT_TYPE), false)
        mv.visitInsn(ARETURN)

        val l1 = Label()
        mv.visitLabel(l1)
        mv.visitLocalVariable("receiver", OBJECT_TYPE.descriptor, null, l0, l1, 0)
        mv.visitMaxs(0, 0)

        mv.visitEnd()

        return TODO()
    }

    override fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable) {
        val v = codegen.v
        val descriptor = resolvedCall.resultingDescriptor
        val valueArguments = resolvedCall.valueArgumentsByIndex ?: error("Failed to arrange value arguments by index: " + descriptor)
        val valueParameters = descriptor.valueParameters

        assert(valueArguments.none { it is DefaultValueArgument }, {
            "Default arguments is not supported in current protocols implementation"
        })

        assert(valueArguments.size == valueParameters.size, {
            "Value arguments collection should have same size, but ${valueArguments.size} != ${valueParameters.size}"
        })

        val shouldMarkLineNumbers = codegen.isShouldMarkLineNumbers
        codegen.isShouldMarkLineNumbers = false

        v.iconst(valueArguments.size)
        v.newarray(OBJECT_TYPE)

        valueArguments.forEachIndexed { index, it ->
            v.dup()
            when (it) {
                is ExpressionValueArgument -> {
                    val right = codegen.gen(it.valueArgument!!.getArgumentExpression())
                    StackValue.arrayElement(OBJECT_TYPE,
                                            StackValue.onStack(Type.getType("[$OBJECT_TYPE")),
                                            StackValue.constant(index, Type.INT_TYPE))
                            .store(right, v)
                }
            }
        }

        codegen.isShouldMarkLineNumbers = shouldMarkLineNumbers
    }
}
