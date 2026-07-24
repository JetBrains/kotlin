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

package org.jetbrains.kotlin.contracts.model

import org.jetbrains.kotlin.K1Deprecation

/**
 * There is a subset of Kotlin language in Effect system (expressions
 *   in right hand side of conditional effect) and [ESExpression] with subtypes
 *   precisely enumerate what can be found here.
 */
@K1Deprecation
interface ESExpression {
    fun <T> accept(visitor: ESExpressionVisitor<T>): T
}

@K1Deprecation
interface ESOperator : ESExpression {
    /**
     * [Functor] that contains logic of concrete operator
     */
    val functor: Functor
}

@K1Deprecation
interface ESValue : Computation, ESExpression
