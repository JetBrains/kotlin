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

package org.jetbrains.kotlin.effectsystem.structure.calltree

import org.jetbrains.kotlin.effectsystem.structure.ESValueID
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.types.KotlinType

/**
 * Classes in this file are nodes of Call Tree-structure, which is
 * essentially tree of nested calls and their arguments
 */

interface CTNode {
    fun <T> accept(visitor: CallTreeVisitor<T>):  T
}

class CTCall(val functor: ESFunctor, val arguments: List<CTNode>) : CTNode {
    override fun <T> accept(visitor: CallTreeVisitor<T>): T = visitor.visitCall(this)
}

interface CTValue : CTNode {
    val id: ESValueID
}

class CTConstant(override val id: ESValueID, val type: KotlinType, val value: Any?): CTValue {
    override fun <T> accept(visitor: CallTreeVisitor<T>): T = visitor.visitConstant(this)
}

class CTVariable(override val id: ESValueID, val type: KotlinType): CTValue {
    override fun <T> accept(visitor: CallTreeVisitor<T>): T = visitor.visitVariable(this)
}

class CTLambda(override val id: ESValueID, val type: KotlinType, val lambdaFunctor: ESFunctor?): CTValue {
    override fun <T> accept(visitor: CallTreeVisitor<T>): T = visitor.visitLambda(this)
}