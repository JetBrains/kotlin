/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.org.objectweb.asm.Type

class KClassJavaObjectTypeProperty : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val receiverValue = resolvedCall!!.extensionReceiver as? ExpressionReceiver ?: return null
        val classLiteralExpression = receiverValue.expression as? KtClassLiteralExpression ?: return null
        val receiverExpression = classLiteralExpression.receiverExpression ?: return null
        val lhs = codegen.bindingContext.get(DOUBLE_COLON_LHS, receiverExpression) ?: return null
        return StackValue.operation(returnType) { iv ->
            if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
                val receiverStackValue = codegen.gen(receiverExpression)
                val extensionReceiverType = receiverStackValue.type
                receiverStackValue.put(extensionReceiverType, iv)
                when {
                    extensionReceiverType == Type.VOID_TYPE -> {
                        iv.aconst(AsmTypes.UNIT_TYPE)
                    }
                    AsmUtil.isPrimitive(extensionReceiverType) ||
                            AsmUtil.unboxPrimitiveTypeOrNull(extensionReceiverType) != null -> {
                        AsmUtil.pop(iv, extensionReceiverType)
                        iv.aconst(AsmUtil.boxType(extensionReceiverType))
                    }
                    else -> {
                        iv.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
                    }
                }
            } else {
                if (TypeUtils.isTypeParameter(lhs.type)) {
                    assert(TypeUtils.isReifiedTypeParameter(lhs.type)) {
                        "Non-reified type parameter under ::class should be rejected by type checker: ${lhs.type}"
                    }
                    codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter(lhs.type, ReifiedTypeInliner.OperationKind.JAVA_CLASS)
                }
                iv.aconst(AsmUtil.boxType(codegen.mapTypeAsDeclaration(lhs.type)))
            }
        }
    }
}
