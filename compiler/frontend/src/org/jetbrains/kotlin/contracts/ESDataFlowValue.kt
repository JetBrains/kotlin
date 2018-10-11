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

import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.structure.ESVariable
import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue

class ESDataFlowValue(descriptor: ValueDescriptor, val dataFlowValue: DataFlowValue) : ESVariable(descriptor) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ESDataFlowValue

        if (dataFlowValue != other.dataFlowValue) return false

        return true
    }

    override fun hashCode(): Int {
        return dataFlowValue.hashCode()
    }
}

class ESLambda(val lambda: KtLambdaExpression) : ESValue(null) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T {
        throw IllegalStateException("Lambdas shouldn't be visited by ESExpressionVisitor")
    }
}