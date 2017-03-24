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

import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_REF_TYPE
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.mapToIndex
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method

abstract class ProtocolGenerator {
    private val generated = mutableSetOf<String>()

    fun putInvokerAndGenerateIfNeeded(method: CallableMethod, call: ResolvedCall<*>) {
        val candidate = call.candidateDescriptor
        val name = "proto$${candidate.containingDeclaration.name}$${candidate.name}$${candidate.valueParameters.joinToString { it.type.toString() }}"

        if (!generated.contains(name)) {
            genProtocolCaller(method, call, name)
            generated.add(name)
        }

        invokeCaller(name)
    }

    abstract fun invokeMethod(method: Callable)

    abstract fun invokeCaller(name: String)

    abstract fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String)

    abstract fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable)
}

class IndyProtocolGenerator(val codegen: ExpressionCodegen, val context: MethodContext) : ProtocolGenerator() {

    override fun invokeCaller(name: String) {
        codegen.v.invokestatic(codegen.parentCodegen.className, name, Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE), false);
    }

    override fun invokeMethod(method: Callable) {
        val args = mutableListOf(OBJECT_TYPE)
        args.addAll(method.valueParameterTypes)

        codegen.v.invokevirtual("java/lang/invoke/MethodHandle", "invoke", Type.getMethodDescriptor(method.returnType, *args.toTypedArray()), false);
    }

    override fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String) {
        val methodName = call.candidateDescriptor.name

        val descriptor = Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE)
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
        mv.visitMethodInsn(INVOKEVIRTUAL, callSite, "getMethod",
                           Type.getMethodDescriptor(Type.getType(MethodHandle::class.java), OBJECT_TYPE), false)
        mv.visitInsn(ARETURN)

        val l1 = Label()
        mv.visitLabel(l1)
        mv.visitLocalVariable("receiver", OBJECT_TYPE.descriptor, null, l0, l1, 0);
        mv.visitMaxs(0, 0)

        mv.visitEnd()
    }

    override fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable) {
        val descriptor = resolvedCall.resultingDescriptor
        val valueArguments = resolvedCall.valueArgumentsByIndex ?: error("Failed to arrange value arguments by index: " + descriptor)
        val argGenerator = CallBasedArgumentGenerator(codegen, generator, descriptor.valueParameters, callableMethod.valueParameterTypes);

        argGenerator.generate(valueArguments, resolvedCall.valueArguments.values.toList(), null)
    }
}

class ReflectionProtocolGenerator(val codegen: ExpressionCodegen) : ProtocolGenerator() {

    override fun invokeMethod(method: Callable) {
        codegen.v.invokevirtual("java/lang/reflect/Method", "invoke", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, Type.getType("[Ljava/lang/Object;")), false)
    }

    override fun invokeCaller(name: String) {
        codegen.v.invokestatic(codegen.parentCodegen.className, name, Type.getMethodDescriptor(Type.getType(Method::class.java), OBJECT_TYPE), false);
    }

    override fun genProtocolCaller(method: CallableMethod, call: ResolvedCall<*>, name: String) {
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
        mv.visitLocalVariable("receiver", OBJECT_TYPE.descriptor, null, l0, l1, 0);
        mv.visitMaxs(0, 0)

        mv.visitEnd()
    }

    override fun putArguments(codegen: ExpressionCodegen, generator: CallGenerator, resolvedCall: ResolvedCall<*>, callableMethod: Callable) {
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

        codegen.v.iconst(valueArguments.size)
        codegen.v.newarray(OBJECT_TYPE)

        valueArguments.forEachIndexed { index, it ->
            codegen.v.dup()
            when (it) {
                is ExpressionValueArgument -> {
                    val right = codegen.gen(it.valueArgument!!.getArgumentExpression())
                    StackValue.arrayElement(OBJECT_TYPE,
                                            StackValue.onStack(Type.getType("[$OBJECT_TYPE")),
                                            StackValue.constant(index, Type.INT_TYPE))
                            .store(right, codegen.v)
                }
            }
        }

        codegen.isShouldMarkLineNumbers = shouldMarkLineNumbers
    }
}
