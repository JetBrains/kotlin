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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.builtins.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class IteratorNext : IntrinsicMethod() {

    private fun getIteratorName(returnType: Type): String {
        return when (returnType) {
            Type.CHAR_TYPE -> "Char"
            Type.BOOLEAN_TYPE -> "Boolean"
            Type.BYTE_TYPE -> "Byte"
            Type.SHORT_TYPE -> "Short"
            Type.INT_TYPE -> "Int"
            Type.LONG_TYPE -> "Long"
            Type.FLOAT_TYPE -> "Float"
            Type.DOUBLE_TYPE -> "Double"
            else -> throw UnsupportedOperationException("Can't get correct name for iterator from type: " + returnType)
        }
    }


    override fun toCallable(method: CallableMethod): Callable {
        val type = AsmUtil.unboxType(method.returnType)
        return object: IntrinsicCallable(type, listOf(), AsmTypes.OBJECT_TYPE, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                val name = getIteratorName(returnType)
                v.invokevirtual(BUILT_INS_PACKAGE_FQ_NAME.toString() + "/" + name + "Iterator", "next" + name, "()" + returnType.getDescriptor(), false)
            }
        }
    }
}
