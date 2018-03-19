/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.org.objectweb.asm.Type

class KClassJavaPrimitiveTypeProperty : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val receiverValue = resolvedCall!!.extensionReceiver as? ExpressionReceiver ?: return null
        val classLiteralExpression = receiverValue.expression as? KtClassLiteralExpression ?: return null
        val receiverExpression = classLiteralExpression.receiverExpression ?: return null
        val lhs = codegen.bindingContext.get(DOUBLE_COLON_LHS, receiverExpression) ?: return null
        val lhsType = codegen.asmType(lhs.type)
        if (TypeUtils.isTypeParameter(lhs.type)) {
            // Reified mechanism is not yet able to generate code that should generate a null value or a field access to TYPE based
            // on a concrete type. To support it, it requires a new kind into ReifiedTypeInliner.OperationKind and add the corresponding
            // rewriting support, thus keep the old mechanism for the moment.
            return null
        }

        return StackValue.operation(returnType) { iv ->
            if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
                val receiverStackValue = codegen.gen(receiverExpression)
                val receiverType = receiverStackValue.type
                when {
                    receiverType == Type.VOID_TYPE -> {
                        receiverStackValue.put(Type.VOID_TYPE, iv)
                        iv.aconst(null)
                    }
                    AsmUtil.isPrimitive(receiverType) -> {
                        receiverStackValue.put(receiverType, iv)
                        AsmUtil.pop(iv, receiverType)
                        iv.getstatic(AsmUtil.boxType(receiverType).getInternalName(), "TYPE", "Ljava/lang/Class;");
                    }
                    else -> {
                        receiverStackValue.put(receiverType, iv)
                        AsmUtil.pop(iv, receiverType)
                        if (AsmUtil.unboxPrimitiveTypeOrNull(receiverType) != null) {
                            iv.getstatic(receiverType.getInternalName(), "TYPE", "Ljava/lang/Class;");
                        } else {
                            iv.aconst(null)
                        }
                    }
                }
            } else {
                if (AsmUtil.isPrimitive(lhsType)
                    || AsmUtil.unboxPrimitiveTypeOrNull(lhsType) != null
                    || AsmTypes.VOID_TYPE.equals(lhsType)
                ) {
                    var type = lhsType
                    if (AsmUtil.isPrimitive(lhsType)) {
                        type = AsmUtil.boxType(lhsType)
                    }
                    iv.getstatic(type.getInternalName(), "TYPE", "Ljava/lang/Class;");
                } else {
                    iv.aconst(null)
                }
            }
        }
    }
}
