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

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

object IteratorNext : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction {
        // If the array element type is unboxed primitive, do not unbox. Otherwise AsmUtil.unbox throws exception
        val type = if (AsmUtil.isBoxedPrimitiveType(signature.returnType)) AsmUtil.unboxType(signature.returnType) else signature.returnType
        val newSignature = signature.newReturnType(type)
        val primitiveClassName = getKotlinPrimitiveClassName(type)
        return IntrinsicFunction.create(expression, newSignature, classCodegen, listOf(getPrimitiveIteratorType(primitiveClassName))) {
            it.invokevirtual(
                getPrimitiveIteratorType(primitiveClassName).internalName,
                "next${primitiveClassName.asString()}",
                "()" + type.descriptor,
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
}
