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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType

public class Inv : IntrinsicMethod() {
    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        assert(isPrimitive(returnType)) { "Return type of Inv intrinsic should be of primitive type : " + returnType }

        receiver.put(numberFunctionOperandType(returnType), v)
        if (returnType == Type.LONG_TYPE) {
            v.lconst(-1)
        }
        else {
            v.iconst(-1)
        }
        v.xor(returnType)
        return returnType
    }

    override fun supportCallable(): Boolean {
        return true
    }

    override fun toCallable(method: CallableMethod): ExtendedCallable {
        val type = numberFunctionOperandType(method.getReturnType())
        return UnaryIntrinsic(method, method.getReturnType(), newThisType = type) {
            if (getReturnType() == Type.LONG_TYPE) {
                it.lconst(-1)
            }
            else {
                it.iconst(-1)
            }
            it.xor(getReturnType())
        }
    }
}
