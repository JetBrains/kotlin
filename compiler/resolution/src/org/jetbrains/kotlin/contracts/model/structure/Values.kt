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

package org.jetbrains.kotlin.contracts.model.structure

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.ESExpression
import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import java.util.*


interface ESReceiver : ESValue {
    val receiverValue: ReceiverValue

    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitReceiver(this)
}


abstract class AbstractESValue(override val type: KotlinType?) : ESValue {
    override val effects: List<ESEffect> = listOf()
}


open class ESReceiverValue(override val receiverValue: ReceiverValue) : AbstractESValue(null), ESReceiver


/**
 * [ESVariable] is class with multiple applications.
 *
 * 1. [ESVariable] represents some variable on declaration-site of contract (reference to parameter
 *   of function). @see [org.jetbrains.kotlin.contracts.interpretation.ContractInterpretationDispatcher.interpretVariable].
 * 2. [ESVariable] is wrapper around argument passed to function in process of substitution.
 *   @see [org.jetbrains.kotlin.contracts.EffectsExtractingVisitor.visitKtElement].
 * 3. [ESVariable] is a key in [Substitutor], that maps values from function signature to
 *   real values from call-site. That keys are equal to variables from point 1.
 *   @see [org.jetbrains.kotlin.contracts.model.functors.SubstitutingFunctor.doInvocation].
 *
 * [ESVariable] at points 2 and 3 must has consistent equality according to using them as keys
 */
open class ESVariable(val descriptor: ValueDescriptor) : AbstractESValue(descriptor.type) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitVariable(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ESVariable

        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int = descriptor.hashCode()

    override fun toString(): String = descriptor.toString()
}


/**
 * [ESConstant] represent some constant is Effect System
 *
 * There is only few constants are supported (@see [ESConstant.Companion])
 */
class ESConstant internal constructor(val constantReference: ConstantReference, override val type: KotlinType) : AbstractESValue(type) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitConstant(this)

    override fun equals(other: Any?): Boolean = other is ESConstant && constantReference == other.constantReference

    override fun hashCode(): Int = Objects.hashCode(constantReference)

    override fun toString(): String = constantReference.name

    fun isNullConstant(): Boolean =
        constantReference == ConstantReference.NULL || constantReference == ConstantReference.NOT_NULL
}

class ESConstants internal constructor(builtIns: KotlinBuiltIns) {
    val trueValue = ESConstant(BooleanConstantReference.TRUE, builtIns.booleanType)
    val falseValue = ESConstant(BooleanConstantReference.FALSE, builtIns.booleanType)
    val nullValue = ESConstant(ConstantReference.NULL, builtIns.nullableNothingType)
    val notNullValue = ESConstant(ConstantReference.NOT_NULL, builtIns.anyType)
    val wildcard = ESConstant(ConstantReference.WILDCARD, builtIns.nullableAnyType)

    fun booleanValue(value: Boolean) =
        if (value) trueValue else falseValue
}

internal val ESExpression.isTrue: Boolean
    get() = this is ESConstant && constantReference == BooleanConstantReference.TRUE

internal val ESExpression.isFalse: Boolean
    get() = this is ESConstant && constantReference == BooleanConstantReference.FALSE

internal val ESValue.isWildcard: Boolean
    get() = this is ESConstant && constantReference == ConstantReference.WILDCARD
