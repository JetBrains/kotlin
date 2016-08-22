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

    private val values = HashMap<Any, IrValue>()

    private inline fun introduceTemporary(irExpression: IrExpression, nameHint: String?, register: (IrValue) -> Unit): IrVariable? {
        val rematerializable = createRematerializableValue(irExpression)
        return if (rematerializable != null) {
            register(rematerializable)
            null
        }
        else {
            createTemporary(irExpression, nameHint, register)
        }
    }

    private inline fun createTemporary(irExpression: IrExpression, nameHint: String?, register: (IrValue) -> Unit): IrVariable {
        val irTemporary = createTemporaryVariable(irExpression, nameHint)
        register(VariableLValue(generator, irTemporary))
        return irTemporary
    }

    private fun createDescriptorForTemporaryVariable(type: KotlinType, nameHint: String? = null): IrTemporaryVariableDescriptor =
            IrTemporaryVariableDescriptorImpl(scopeOwner, Name.identifier(getNameForTemporary(nameHint)), type)

    private fun getNameForTemporary(nameHint: String?): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
    }

    fun createTemporaryVariable(irExpression: IrExpression, nameHint: String? = null): IrVariable =
            IrVariableImpl(
                    irExpression.startOffset, irExpression.endOffset, IrDeclarationOriginKind.IR_TEMPORARY_VARIABLE,
                    createDescriptorForTemporaryVariable(
                            irExpression.type ?: throw AssertionError("No type for $irExpression"),
                            nameHint
                    ),
                    irExpression
            )

    fun introduceTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String? = null): IrVariable? =
            introduceTemporary(irExpression, nameHint) { putValue(ktExpression, it) }

    fun introduceTemporary(valueParameterDescriptor: ValueParameterDescriptor, irExpression: IrExpression): IrVariable? =
            introduceTemporary(irExpression, valueParameterDescriptor.name.asString()) { putValue(valueParameterDescriptor, it) }

    fun introduceTemporary(irExpression: IrExpression): IrVariable? =
            introduceTemporary(irExpression, null) { putValue(irExpression, it) }

    fun createTemporary(ktExpression: KtExpression, irExpression: IrExpression, nameHint: String?): IrVariable =
            createTemporary(irExpression, nameHint) { putValue(ktExpression, it) }

    fun putValue(ktExpression: KtExpression, irValue: IrValue) {
        values[ktExpression] = irValue
    }

    fun putValue(receiver: ReceiverValue, irValue: IrValue) {
        values[receiver] = irValue
    }

    fun putValue(parameter: ValueParameterDescriptor, irValue: IrValue) {
        values[parameter] = irValue
    }

    fun putValue(irExpression: IrExpression, irValue: IrValue) {
        values[irExpression] = irValue
    }

    fun valueOf(ktExpression: KtExpression): IrExpression? =
            values[ktExpression]?.load() ?: parent?.valueOf(ktExpression)

    fun valueOf(receiver: ReceiverValue): IrExpression? =
            values[receiver]?.load() ?: parent?.valueOf(receiver)

    fun valueOf(parameter: ValueParameterDescriptor): IrExpression? =
            values[parameter]?.load() ?: parent?.valueOf(parameter)

    fun valueOf(irExpression: IrExpression): IrExpression? =
            values[irExpression]?.load() ?: parent?.valueOf(irExpression)

    companion object {
        fun rootScope(scopeOwner: DeclarationDescriptor, generator: IrBodyGenerator): Scope {
            val scope = Scope(scopeOwner)
            scope.generator = generator
            return scope
        }
    }
}
