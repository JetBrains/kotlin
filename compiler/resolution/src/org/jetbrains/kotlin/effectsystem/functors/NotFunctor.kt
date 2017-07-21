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
import org.jetbrains.kotlin.effectsystem.factories.createClause
import org.jetbrains.kotlin.effectsystem.factories.negated
import org.jetbrains.kotlin.effectsystem.impls.ESBooleanConstant
import org.jetbrains.kotlin.effectsystem.structure.ESClause
import org.jetbrains.kotlin.utils.addToStdlib.cast

class NotFunctor : AbstractSequentialUnaryFunctor() {
    override fun combineClauses(list: List<ESClause>): List<ESClause> = list.mapNotNull {
        val outcome = it.effect

        // Outcome guaranteed to be Returns by AbstractSequentialUnaryFunctor, but cast
        // to boolean constant can fail in case of type-errors in the whole expression,
        // like "foo(bar) && 1"
        val booleanValue = outcome.cast<ESReturns>().value as? ESBooleanConstant ?: return@mapNotNull null

        return@mapNotNull createClause(it.condition, ESReturns(booleanValue.negated()))
    }
}