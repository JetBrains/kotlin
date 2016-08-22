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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginKind
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi2ir.generators.values.IrValue
import org.jetbrains.kotlin.psi2ir.generators.values.VariableLValue
import org.jetbrains.kotlin.psi2ir.generators.values.createRematerializableValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class Scope private constructor(val scopeOwner: DeclarationDescriptor, val parent: Scope? = null) {
    internal lateinit var generator: IrBodyGenerator

    constructor(parent: Scope) : this(parent.scopeOwner, parent) {
        this.generator = parent.generator
    }

    private var lastTemporaryIndex: Int = parent?.lastTemporaryIndex ?: 0
    private fun nextTemporaryIndex(): Int = parent?.nextTemporaryIndex() ?: lastTemporaryIndex++

    private val expressionValues = HashMap<KtExpression, IrValue>()
    private val receiverValues = HashMap<ReceiverValue, IrValue>()
    private val valueArgumentValues = HashMap<ValueParameterDescriptor, IrValue>()

    private fun createDescriptorForTemporaryVariable(type: KotlinType, nameHint: String? = null): IrTemporaryVariableDescriptor =
            IrTemporaryVariableDescriptorImpl(
                    scopeOwner,
                    Name.identifier(
                            if (nameHint != null)
                                "tmp${nextTemporaryIndex()}_$nameHint"
                            else
                                "tmp${nextTemporaryIndex()}"
                    ),
                    type)

    fun createTemporaryVariable(irExpression: IrExpression, nameHint: String? = null): IrVariable =
            IrVariableImpl(irExpression.startOffset, irExpression.endOffset, IrDeclarationOriginKind.IR_TEMPORARY_VARIABLE,
                           createDescriptorForTemporaryVariable(
                                   irExpression.type ?: throw AssertionError("No type for $irExpression"),
                                   nameHint
                           ),
                           irExpression)

    fun introduceTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String? = null): IrVariable? {
        val rematerializable = createRematerializableValue(irExpression)
        if (rematerializable != null) {
            putValue(ktExpression, rematerializable)
            return null
        }

        return createTemporary(ktExpression, irExpression, nameHint)
    }

    fun createTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String?): IrVariable {
        val irTmpVar = createTemporaryVariable(irExpression, nameHint)
        putValue(ktExpression, VariableLValue(generator, irTmpVar))
        return irTmpVar
    }

    fun createTemporary(irExpression: IrExpression, nameHint: String?): IrVariable =
            createTemporaryVariable(irExpression, nameHint)

    fun introduceTemporary(valueParameterDescriptor: ValueParameterDescriptor, irExpression: IrExpression): IrVariable? {
        val rematerializable = createRematerializableValue(irExpression)
        if (rematerializable != null) {
            putValue(valueParameterDescriptor, rematerializable)
            return null
        }

        val irTmpVar = createTemporaryVariable(irExpression, valueParameterDescriptor.name.asString())
        putValue(valueParameterDescriptor, VariableLValue(generator, irTmpVar))
        return irTmpVar
    }

    fun putValue(ktExpression: KtExpression, irValue: IrValue) {
        expressionValues[ktExpression] = irValue
    }

    fun putValue(receiver: ReceiverValue, irValue: IrValue) {
        receiverValues[receiver] = irValue
    }

    fun putValue(parameter: ValueParameterDescriptor, irValue: IrValue) {
        valueArgumentValues[parameter] = irValue
    }

    fun valueOf(ktExpression: KtExpression): IrExpression? =
            expressionValues[ktExpression]?.load() ?: parent?.valueOf(ktExpression)
    fun valueOf(receiver: ReceiverValue): IrExpression? =
            receiverValues[receiver]?.load() ?: parent?.valueOf(receiver)
    fun valueOf(parameter: ValueParameterDescriptor): IrExpression? =
            valueArgumentValues[parameter]?.load() ?: parent?.valueOf(parameter)

    companion object {
        fun rootScope(scopeOwner: DeclarationDescriptor, generator: IrBodyGenerator): Scope {
            val scope = Scope(scopeOwner)
            scope.generator = generator
            return scope
        }
    }
}
