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

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class KClassJavaProperty : IntrinsicPropertyGetter() {
    override fun generate(resolvedCall: ResolvedCall<*>?, codegen: ExpressionCodegen, returnType: Type, receiver: StackValue): StackValue? {
        val type = resolvedCall!!.extensionReceiver.type.arguments.single().type
        val asmType = codegen.state.typeMapper.mapType(type)

        return when {
            isReifiedTypeParameter(type) -> {
                StackValue.operation(returnType) { iv ->
                    codegen.putReifierMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.JAVA_CLASS_MARKER_METHOD_NAME)
                    AsmUtil.putJavaLangClassInstance(iv, asmType)
                    coerceToJavaLangClass(iv, returnType)
                }
            }
            isWithClassLiteralArgument(resolvedCall) -> {
                StackValue.operation(returnType) { iv ->
                    if (AsmUtil.isPrimitive(asmType)) {
                        iv.getstatic(AsmUtil.boxType(asmType).internalName, "TYPE", "Ljava/lang/Class;")
                    }
                    else {
                        iv.tconst(asmType)
                    }
                    coerceToJavaLangClass(iv, returnType)
                }
            }
            else -> null
        }
    }

    private fun isReifiedTypeParameter(type: KotlinType): Boolean {
        val typeDescriptor = type.constructor.declarationDescriptor
        return typeDescriptor is TypeParameterDescriptor && typeDescriptor.isReified
    }

    private fun isWithClassLiteralArgument(resolvedCall: ResolvedCall<*>): Boolean {
        val extensionReceiver = resolvedCall.extensionReceiver
        return extensionReceiver is ExpressionReceiver && extensionReceiver.expression is KtClassLiteralExpression
    }

    private fun coerceToJavaLangClass(iv: InstructionAdapter, returnType: Type) {
        StackValue.coerce(AsmTypes.JAVA_CLASS_TYPE, returnType, iv)
    }
}
