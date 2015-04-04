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

import com.sun.org.apache.bcel.internal.generic.INVOKESPECIAL
import com.sun.org.apache.bcel.internal.generic.INVOKESTATIC
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.Opcodes.*;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.util.Printer;

import java.util.ArrayList;

public class CallableMethod(private val owner: Type, private val defaultImplOwner: Type?, private val defaultImplParam: Type?, private val signature: JvmMethodSignature, private val invokeOpcode: Int, private val thisClass: Type?, private val receiverParameterType: Type?, private val generateCalleeType: Type?) : ExtendedCallable {

    override fun getOwner(): Type {
        return owner
    }

    public fun getValueParameters(): List<JvmMethodParameterSignature> {
        return signature.getValueParameters()
    }

    override fun getValueParameterTypes(): List<Type> {
        val valueParameters = signature.getValueParameters()
        val result = ArrayList<Type>(valueParameters.size())
        for (parameter in valueParameters) {
            if (parameter.getKind() == JvmMethodParameterKind.VALUE) {
                result.add(parameter.getAsmType())
            }
        }
        return result
    }

    public fun getAsmMethod(): Method {
        return signature.getAsmMethod()
    }

    override fun getArgumentTypes(): Array<Type> {
        return signature.getAsmMethod().getArgumentTypes()
    }

    override fun getThisType(): Type? {
        return thisClass
    }

    override fun getReceiverClass(): Type? {
        return receiverParameterType
    }

    private fun invoke(v: InstructionAdapter) {
        v.visitMethodInsn(invokeOpcode, owner.getInternalName(), getAsmMethod().getName(), getAsmMethod().getDescriptor())
    }


    public override fun invokeWithNotNullAssertion(v: InstructionAdapter, state: GenerationState, resolvedCall: ResolvedCall<*>) {
        invokeWithoutAssertions(v)
        AsmUtil.genNotNullAssertionForMethod(v, state, resolvedCall)
    }

    public override fun invokeWithoutAssertions(v: InstructionAdapter) {
        invoke(v)
    }

    override fun getGenerateCalleeType(): Type? {
        return generateCalleeType
    }

    private fun invokeDefault(v: InstructionAdapter) {
        if (defaultImplOwner == null || defaultImplParam == null) {
            throw IllegalStateException()
        }

        val method = getAsmMethod();
        val desc = JetTypeMapper.getDefaultDescriptor(method,
                                                         if (invokeOpcode == INVOKESTATIC) null else defaultImplParam.getDescriptor(),
                                                         receiverParameterType != null);

        if ("<init>".equals(method.getName())) {
            v.aconst(null)
            v.visitMethodInsn(INVOKESPECIAL, defaultImplOwner!!.getInternalName(), "<init>", desc, false)
        }
        else {
            v.visitMethodInsn(INVOKESTATIC, defaultImplOwner.getInternalName(),
                              method.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc, false);
        }
    }

    public fun invokeDefaultWithNotNullAssertion(v: InstructionAdapter, state: GenerationState, resolvedCall: ResolvedCall<*>) {
        invokeDefault(v)
        AsmUtil.genNotNullAssertionForMethod(v, state, resolvedCall)
    }

    override fun getReturnType(): Type {
        return signature.getReturnType()
    }

    override fun toString(): String {
        return Printer.OPCODES[invokeOpcode] + " " + owner.getInternalName() + "." + signature
    }

    override fun isStaticCall(): Boolean {
        return invokeOpcode == INVOKESTATIC
    }


    public override fun beforeParameterGeneration(v: InstructionAdapter, value: StackValue?) {

    }

    override fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, returnType: Type, codegen: ExpressionCodegen): StackValue {
        return StackValue.functionCall(returnType) {
            codegen.invokeMethodWithArguments(this@CallableMethod, resolvedCall, receiver, returnType)
        }
    }
}
