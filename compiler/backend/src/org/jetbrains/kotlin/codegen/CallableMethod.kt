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

public class CallableMethod(override val owner: Type, private val defaultImplOwner: Type?, private val defaultImplParam: Type?, private val signature: JvmMethodSignature, private val invokeOpcode: Int, override val dispatchReceiverType: Type?, override val extensionReceiverType: Type?, override val generateCalleeType: Type?) : Callable {

    public fun getValueParameters(): List<JvmMethodParameterSignature> {
        return signature.getValueParameters()
    }

    override val valueParameterTypes: List<Type>
        get() = signature.getValueParameters().filter { it.getKind() == JvmMethodParameterKind.VALUE }.map { it.getAsmType() }

    public fun getAsmMethod(): Method {
        return signature.getAsmMethod()
    }

    override val parameterTypes: Array<Type>
        get() = getAsmMethod().getArgumentTypes()


    public override fun genInvokeInstruction(v: InstructionAdapter) {
        v.visitMethodInsn(invokeOpcode, owner.getInternalName(), getAsmMethod().getName(), getAsmMethod().getDescriptor())
    }

    public fun genInvokeDefaultInstruction(v: InstructionAdapter) {
        if (defaultImplOwner == null || defaultImplParam == null) {
            throw IllegalStateException()
        }

        val method = getAsmMethod();
        val desc = JetTypeMapper.getDefaultDescriptor(method,
                                                      if (invokeOpcode == INVOKESTATIC) null else defaultImplParam.getDescriptor(),
                                                      extensionReceiverType != null);

        if ("<init>".equals(method.getName())) {
            v.aconst(null)
            v.visitMethodInsn(INVOKESPECIAL, defaultImplOwner!!.getInternalName(), "<init>", desc, false)
        }
        else {
            v.visitMethodInsn(INVOKESTATIC, defaultImplOwner.getInternalName(),
                              method.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc, false);
        }
    }

    override val returnType: Type
        get() = signature.getReturnType()

    override fun isStaticCall(): Boolean {
        return invokeOpcode == INVOKESTATIC
    }

    override fun toString(): String {
        return "${Printer.OPCODES[invokeOpcode]} ${owner.getInternalName()}.$signature"
    }
}
