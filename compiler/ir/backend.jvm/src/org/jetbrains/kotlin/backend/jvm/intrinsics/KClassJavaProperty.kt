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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.codegen.AsmUtil.putJavaLangClassInstance
import org.jetbrains.kotlin.codegen.AsmUtil.wrapJavaClassIntoKClass
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class KClassJavaProperty : IntrinsicMethod() {
    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        return object: IrIntrinsicFunction(expression, signature, context) {
            override fun invoke(v: InstructionAdapter, codegen: org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen, data: BlockInfo): StackValue {
                putJavaLangClassInstance(v, signature.returnType)
                wrapJavaClassIntoKClass(v)
                return StackValue.onStack(signature.returnType)
            }
        }


//        val type = lhs.type
//        if (lhs is DoubleColonLHS.Expression && !(lhs as DoubleColonLHS.Expression).isObject) {
//            JavaClassProperty.generateImpl(v, gen(receiverExpression))
//        }
//        else {
//            if (TypeUtils.isTypeParameter(type)) {
//                assert(TypeUtils.isReifiedTypeParameter(type)) { "Non-reified type parameter under ::class should be rejected by type checker: " + type }
//                putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.JAVA_CLASS)
//            }

//            putJavaLangClassInstance(v, typeMapper.mapType(type))
//        }

//        if (wrapIntoKClass) {
//            wrapJavaClassIntoKClass(v)
//        }

//        return Unit
    }


    fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val receiverValue = resolvedCall!!.extensionReceiver as? ExpressionReceiver ?: return null
        val classLiteralExpression = receiverValue.expression as? KtClassLiteralExpression ?: return null
        val receiverExpression = classLiteralExpression.receiverExpression ?: return null
        val lhs = codegen.bindingContext.get(DOUBLE_COLON_LHS, receiverExpression) ?: return null
        val value = codegen.generateClassLiteralReference(lhs, receiverExpression, /* wrapIntoKClass = */ false)
        return StackValue.coercion(value, returnType)
    }
}
