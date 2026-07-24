/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.contracts.description.expressions

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.contracts.description.BooleanExpression
import org.jetbrains.kotlin.contracts.description.ContractDescriptionElement
import org.jetbrains.kotlin.contracts.description.ContractDescriptionVisitor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor


@K1Deprecation
interface ContractDescriptionValue : ContractDescriptionElement {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitValue(this, data)
}

@K1Deprecation
open class ConstantReference(val name: String) : ContractDescriptionValue {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitConstantDescriptor(this, data)

    companion object {
        val NULL = ConstantReference("NULL")
        val WILDCARD = ConstantReference("WILDCARD")
        val NOT_NULL = ConstantReference("NOT_NULL")
    }
}

@K1Deprecation
class BooleanConstantReference(name: String) : ConstantReference(name), BooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanConstantDescriptor(this, data)

    companion object {
        val TRUE = BooleanConstantReference("TRUE")
        val FALSE = BooleanConstantReference("FALSE")
    }
}

@K1Deprecation
open class VariableReference(val descriptor: ParameterDescriptor) : ContractDescriptionValue {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D) =
        contractDescriptionVisitor.visitVariableReference(this, data)
}

@K1Deprecation
class BooleanVariableReference(descriptor: ParameterDescriptor) : VariableReference(descriptor), BooleanExpression {
    override fun <R, D> accept(contractDescriptionVisitor: ContractDescriptionVisitor<R, D>, data: D): R =
        contractDescriptionVisitor.visitBooleanVariableReference(this, data)
}