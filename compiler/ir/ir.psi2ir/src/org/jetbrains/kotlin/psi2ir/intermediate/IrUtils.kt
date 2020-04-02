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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor

fun IrVariable.defaultLoad(): IrExpression =
    IrGetValueImpl(startOffset, endOffset, type, symbol)

fun CallReceiver.adjustForCallee(callee: CallableMemberDescriptor): CallReceiver =
    object : CallReceiver {
        override fun call(withDispatchAndExtensionReceivers: (IntermediateValue?, IntermediateValue?) -> IrExpression): IrExpression =
            this@adjustForCallee.call { dispatchReceiverValue, extensionReceiverValue ->
                val numReceiversPresent = listOfNotNull(dispatchReceiverValue, extensionReceiverValue).size
                val numReceiversExpected = listOfNotNull(callee.dispatchReceiverParameter, callee.extensionReceiverParameter).size
                if (numReceiversPresent != numReceiversExpected)
                    throw AssertionError("Mismatching receivers for $callee: $numReceiversPresent, expected: $numReceiversExpected")

                val newDispatchReceiverValue =
                    when {
                        callee.dispatchReceiverParameter == null -> null
                        dispatchReceiverValue != null -> dispatchReceiverValue
                        else -> extensionReceiverValue
                    }
                val newExtensionReceiverValue =
                    when {
                        callee.extensionReceiverParameter == null -> null
                        dispatchReceiverValue != null && callee.dispatchReceiverParameter == null -> dispatchReceiverValue
                        else -> extensionReceiverValue
                    }
                withDispatchAndExtensionReceivers(newDispatchReceiverValue, newExtensionReceiverValue)
            }
    }


fun computeSubstitutedSyntheticAccessor(propertyDescriptor: PropertyDescriptor, accessorFunctionDescriptor: FunctionDescriptor): FunctionDescriptor {
    if (propertyDescriptor.original == propertyDescriptor) return accessorFunctionDescriptor

    // Compute substituted accessor descriptor in case of Synthetic Java property `Java: getFoo() -> Kotlin: foo`
    if (propertyDescriptor !is SyntheticPropertyDescriptor) return accessorFunctionDescriptor

    if (propertyDescriptor.extensionReceiverParameter == null || propertyDescriptor.dispatchReceiverParameter != null) {
        return accessorFunctionDescriptor
    }

    if (accessorFunctionDescriptor !is SimpleFunctionDescriptor) return accessorFunctionDescriptor

    /**
     * Consider java class like
     * O.java
     * class O<T1> {
     *   class I<T2> {
     *     public String getFoo() { ... }
     *   }
     * }
     *
     * k.kt
     * fun f(i: O<String>.I<Int>) = i.foo
     *
     *
     * for that case front end creates synthetic property foo
     * private val <T1', T2'> O<T1'>.I<T2'>.foo get() = this.getFoo()
     *
     * So to get substituted descriptor for getFoo() we need to extract substitution from extension receiver type.
     * To do so map Tx parameters onto Tx' arguments like
     * T1' -> String
     * T2' -> Int
     * into
     * T1 -> String
     * T2 -> Int
     */

    val classDescriptor = accessorFunctionDescriptor.containingDeclaration as ClassDescriptor
    val collectedTypeParameters = classDescriptor.typeConstructor.parameters.map { it.typeConstructor }
    assert(collectedTypeParameters.size == propertyDescriptor.typeParametersCount)

    val typeArguments = propertyDescriptor.extensionReceiverParameter!!.type.arguments
    assert(typeArguments.size == collectedTypeParameters.size)

    val typeSubstitutor = TypeSubstitutor.create(collectedTypeParameters.zip(typeArguments).toMap())
    return accessorFunctionDescriptor.substitute(typeSubstitutor) ?: error("Cannot substitute descriptor for $accessorFunctionDescriptor")
}