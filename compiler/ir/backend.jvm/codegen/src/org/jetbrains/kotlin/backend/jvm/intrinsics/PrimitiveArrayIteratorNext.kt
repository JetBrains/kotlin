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

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod.Companion.newReturnType
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object PrimitiveArrayIteratorNext : IntrinsicMethod() {

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IrIntrinsicFunction {
        // If the array element type is unboxed primitive, do not unbox. Otherwise AsmUtil.unbox throws exception
        val type = if (AsmUtil.isBoxedPrimitiveType(signature.returnType)) AsmUtil.unboxType(signature.returnType) else signature.returnType
        return createIntrinsicForPrimitiveIteratorNext(expression, signature, classCodegen, type)
    }
}

object VArrayIteratorNext : IntrinsicMethod() {

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? =
        with(codegen) {
            val descriptor = methodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
            val stackValue = toCallable(expression, descriptor, codegen.classCodegen).invoke(mv, codegen, data, expression)
            stackValue.put(mv)
            return MaterialValue(this, codegen.typeMapper.mapType(expression.type), expression.type)
        }

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IrIntrinsicFunction {
        val iteratorTypeArg = (expression.dispatchReceiver!!.type as? IrSimpleType)?.arguments?.get(0)?.typeOrNull
        val iteratorTypeArgMapping = iteratorTypeArg?.let { classCodegen.typeMapper.mapType(it) }
        if (iteratorTypeArgMapping != null && AsmUtil.isPrimitive(iteratorTypeArgMapping)) {
            return createIntrinsicForPrimitiveIteratorNext(expression, signature, classCodegen, iteratorTypeArgMapping)
        }
        return IrIntrinsicFunction.create(expression, signature, classCodegen, Type.getObjectType(vArrayIteratorType)) {
            it.invokeinterface(
                vArrayIteratorType,
                "next",
                signature.asmMethod.descriptor
            )
            it.checkcast(classCodegen.typeMapper.mapType(expression.type))
        }
    }
}

private fun createIntrinsicForPrimitiveIteratorNext(
    expression: IrFunctionAccessExpression,
    signature: JvmMethodSignature,
    classCodegen: ClassCodegen,
    primitiveType: Type
): IrIntrinsicFunction {
    val primitiveClassName = getKotlinPrimitiveClassName(primitiveType)

    return IrIntrinsicFunction.create(
        expression,
        signature.newReturnType(primitiveType),
        classCodegen,
        getPrimitiveIteratorType(primitiveClassName)
    ) {
        it.invokevirtual(
            getPrimitiveIteratorType(primitiveClassName).internalName,
            "next${primitiveClassName.asString()}",
            "()${primitiveType.descriptor}",
            false
        )
    }
}

// Type.CHAR_TYPE -> "Char"
private fun getKotlinPrimitiveClassName(type: Type): Name {
    return JvmPrimitiveType.get(type.className).primitiveType.typeName
}

// "Char" -> type for kotlin.collections.CharIterator
private fun getPrimitiveIteratorType(primitiveClassName: Name): Type {
    val iteratorName = Name.identifier(primitiveClassName.asString() + "Iterator")
    return Type.getObjectType(COLLECTIONS_PACKAGE_FQ_NAME.child(iteratorName).internalNameWithoutInnerClasses)
}

private const val vArrayIteratorType = "kotlin/VArrayIterator"