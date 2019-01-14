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


/**
 * [ESExpression] is entity used by Effect System
 *
 * All those entities can be visited by [ESExpressionVisitor], so
 *   they can be:
 *   - reduced by [org.jetbrains.kotlin.contracts.model.visitors.Reducer]
 *   - substituted with real values by [org.jetbrains.kotlin.contracts.model.visitors.Substitutor]
 *   - collected to [MutableContextInfo] by [org.jetbrains.kotlin.contracts.model.visitors.InfoCollector]
 */
interface ESExpression {
    fun <T> accept(visitor: ESExpressionVisitor<T>): T
}

/**
 * [ESOperator] is [ESExpression] represents operator on
 *   one or more ESExpressions
 */
interface ESOperator : ESExpression {
    /**
     * [Functor] that contains logic of concrete operator
     */
    val functor: Functor
}

/**
 * [ESValue] is [ESExpression] that can be a [Computation]
 */
interface ESValue : Computation, ESExpression