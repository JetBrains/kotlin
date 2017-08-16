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

package org.jetbrains.kotlin.effectsystem.functors

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.factories.boundSchemaFromClauses
import org.jetbrains.kotlin.effectsystem.structure.ESClause
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.effectsystem.structure.EffectSchema

/**
 * Binary functor that has sequential semantics, i.e. it won't apply to
 * computations that can't be guaranteed to be finished.
 *
 * It provides [combineClauses] method for successors, which is guaranteed to
 * be called only on clauses that haven't failed before reaching functor transformation.
 */
abstract class AbstractSequentialBinaryFunctor : ESFunctor {
    override fun apply(arguments: List<EffectSchema>): EffectSchema? {
        assert(arguments.size == 2, { "Wrong size of arguments list for Binary operator: expected 2, got ${arguments.size}" })
        return apply(arguments[0], arguments[1])
    }

    fun apply(left: EffectSchema, right: EffectSchema): EffectSchema? {
        val (leftReturning, leftRest) = left.clauses.partition { it.effect is ESReturns }
        val (rightReturning, rightRest) = right.clauses.partition { it.effect is ESReturns }

        // Traces that evaluated both arguments and went to the functor
        val evaluatedByFunctor = combineClauses(leftReturning, rightReturning)

        return boundSchemaFromClauses(leftRest + rightRest + evaluatedByFunctor)
    }

    abstract fun combineClauses(left: List<ESClause>, right: List<ESClause>): List<ESClause>
}