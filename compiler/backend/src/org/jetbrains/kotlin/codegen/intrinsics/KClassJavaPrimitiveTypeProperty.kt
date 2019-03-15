/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.BindingContext.DOUBLE_COLON_LHS
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.isInlineClassType
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
        if (TypeUtils.isTypeParameter(lhs.type)) {
            // TODO: add new operation kind to ReifiedTypeInliner.OperationKind to generate a null value or a field access to TYPE
            return null
        }
        if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
            val receiverType = codegen.bindingContext.getType(receiverExpression) ?: return null
            if (!KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType(receiverType)) return null
        }

        val lhsType = codegen.asmType(lhs.type)
        return StackValue.operation(returnType) { iv ->
            when {
                lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier -> {
                    codegen.gen(receiverExpression).put(lhsType, iv)
                    AsmUtil.pop(iv, lhsType)
                    if (lhs.type.isInlineClassType())
                        iv.aconst(null)
                    else
                        iv.getstatic(AsmUtil.boxType(lhsType).internalName, "TYPE", "Ljava/lang/Class;")
                }

                !lhs.type.isInlineClassType() && isPrimitiveOrWrapper(lhsType) -> {
                    iv.getstatic(AsmUtil.boxType(lhsType).internalName, "TYPE", "Ljava/lang/Class;")
                }

                else -> iv.aconst(null)
            }
        }
    }

    private fun isPrimitiveOrWrapper(lhsType: Type) =
        AsmUtil.isPrimitive(lhsType) || AsmUtil.unboxPrimitiveTypeOrNull(lhsType) != null || AsmTypes.VOID_WRAPPER_TYPE == lhsType
}
