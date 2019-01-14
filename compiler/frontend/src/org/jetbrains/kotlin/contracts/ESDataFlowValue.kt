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

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.model.structure.AbstractESValue
import org.jetbrains.kotlin.contracts.model.structure.ESReceiverValue
import org.jetbrains.kotlin.contracts.model.structure.ESVariable
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue


/**
 * [ESDataFlow] is an interface represents some entity that holds [DataFlowValue] for DFA.
 *   All [ESDataFlow] must implement [equals] by [dataFlowValue], so for that there is
 *   a function [dataFlowEquals] to avoid copy-paste.

 * Actually that interfaces must be sealed.
 */
interface ESDataFlow {
    val dataFlowValue: DataFlowValue

    fun dataFlowEquals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ESDataFlow) return false

        return dataFlowValue == other.dataFlowValue
    }
}


/**
 * [ESDataFlowValue] is [ESVariable] with data flow information.
 */
class ESDataFlowValue(
    descriptor: ValueDescriptor,
    override val dataFlowValue: DataFlowValue
) : ESVariable(descriptor), ESDataFlow {
    override fun equals(other: Any?): Boolean = dataFlowEquals(other)

    override fun hashCode(): Int {
        return dataFlowValue.hashCode()
    }
}


/**
 * [ESDataFlowReceiver] is [ESReceiverValue] with data flow information.
 */
class ESDataFlowReceiver(
    receiverValue: ReceiverValue,
    override val dataFlowValue: DataFlowValue
) : ESReceiverValue(receiverValue), ESDataFlow {
    override fun equals(other: Any?): Boolean = dataFlowEquals(other)

    override fun hashCode(): Int {
        return dataFlowValue.hashCode()
    }
}


/**
 * [ESLambda] represents lambda functions in Effect System
 */
class ESLambda(val lambda: KtLambdaExpression, val receiverValue: ReceiverValue?) : AbstractESValue(null) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T {
        throw IllegalStateException("Lambdas shouldn't be visited by ESExpressionVisitor")
    }
}