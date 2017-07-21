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

package org.jetbrains.kotlin.effectsystem.impls

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.effectsystem.structure.ESValueID
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanValue
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.effectsystem.structure.ESExpressionVisitor
import org.jetbrains.kotlin.types.KotlinType

open class ESVariable(override val id: ESValueID, val type: KotlinType) : ESValue {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitVariable(this)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ESVariable

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id.toString()
}

class ESBooleanVariable(id: ESValueID) : ESVariable(id, DefaultBuiltIns.Instance.booleanType), ESBooleanValue {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitBooleanVariable(this)
}

class ESLambda(id: ESValueID, type: KotlinType) : ESVariable(id, type) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitLambda(this)
}

open class ESConstant(override val id: ESValueID, open val value: Any?, val type: KotlinType) : ESValue {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitConstant(this)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ESConstant

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id.toString()
}

class ESBooleanConstant(id: ESValueID, override val value: Boolean) : ESConstant(id, value, DefaultBuiltIns.Instance.booleanType), ESBooleanValue {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitBooleanConstant(this)
}




