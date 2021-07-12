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

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

fun generateParameterNames(
        functionDescriptor: FunctionDescriptor,
        mv: MethodVisitor,
        jvmSignature: JvmMethodSignature,
        state: GenerationState,
        isSynthetic: Boolean
) {
    if (!state.generateParametersMetadata || isSynthetic) {
        return
    }

    val iterator = functionDescriptor.valueParameters.iterator()
    val kotlinParameterTypes = jvmSignature.valueParameters
    var isEnumName = true

    for ((index, parameterSignature) in kotlinParameterTypes.withIndex()) {
        val kind = parameterSignature.kind

        val name = when (kind) {
            JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL -> {
                isEnumName = !isEnumName
                if (!isEnumName) "\$enum\$name" else "\$enum\$ordinal"
            }
            JvmMethodParameterKind.RECEIVER,
            JvmMethodParameterKind.CONTEXT_RECEIVER -> {
                DescriptorAsmUtil.getNameForReceiverParameter(functionDescriptor, state.bindingContext, state.languageVersionSettings)
            }
            JvmMethodParameterKind.OUTER -> AsmUtil.CAPTURED_THIS_FIELD
            JvmMethodParameterKind.VALUE -> iterator.next().name.asString()

            JvmMethodParameterKind.CONSTRUCTOR_MARKER,
            JvmMethodParameterKind.SUPER_CALL_PARAM,
            JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE,
            JvmMethodParameterKind.THIS -> {
                //we can't generate null name cause of jdk problem #9045294
                "arg" + index
            }
        }

        //A construct emitted by a Java compiler must be marked as synthetic if it does not correspond to a construct declared explicitly or
        // implicitly in source code, unless the emitted construct is a class initialization method (JVMS §2.9).
        //A construct emitted by a Java compiler must be marked as mandated if it corresponds to a formal parameter
        // declared implicitly in source code (§8.8.1, §8.8.9, §8.9.3, §15.9.5.1).
        val access = when (kind) {
            JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL -> Opcodes.ACC_SYNTHETIC
            JvmMethodParameterKind.RECEIVER, JvmMethodParameterKind.CONTEXT_RECEIVER -> Opcodes.ACC_MANDATED
            JvmMethodParameterKind.OUTER -> Opcodes.ACC_MANDATED
            JvmMethodParameterKind.VALUE -> 0

            JvmMethodParameterKind.CONSTRUCTOR_MARKER,
            JvmMethodParameterKind.SUPER_CALL_PARAM,
            JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE,
            JvmMethodParameterKind.THIS -> Opcodes.ACC_SYNTHETIC
        }

        mv.visitParameter(name, access)
    }
}
