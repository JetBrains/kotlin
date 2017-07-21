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
 * Unary functor that has sequential semantics, i.e. it won't apply to
 * computations that can't be guaranteed to be finished.
 *
 *  It provides [combineClauses] method for successors, which is guaranteed to
 * be called only on clauses that haven't failed before reaching functor transformation.
 */
abstract class AbstractSequentialUnaryFunctor : ESFunctor {
    override fun apply(arguments: List<EffectSchema>): EffectSchema? {
        assert(arguments.size == 1, { "Wrong size of arguments list for Unary operator: expected 1, got ${arguments.size}" })
        return apply(arguments[0])
    }

    fun apply(arg: EffectSchema): EffectSchema? {
        val (returning, rest) = arg.clauses.partition { it.effect is ESReturns }

        val evaluatedByFunctor = combineClauses(returning)

        return boundSchemaFromClauses(rest + evaluatedByFunctor)
    }

    abstract fun combineClauses(list: List<ESClause>): List<ESClause>
}

