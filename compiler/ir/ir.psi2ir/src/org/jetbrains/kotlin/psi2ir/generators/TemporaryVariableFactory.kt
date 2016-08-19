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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginKind
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

class TemporaryVariableFactory(val scopeOwner: DeclarationDescriptor) {
    private var lastTemporaryIndex = 0
    private fun nextTemporaryIndex() = lastTemporaryIndex++

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
}
