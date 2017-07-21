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

package org.jetbrains.kotlin.effectsystem.resolving

import org.jetbrains.kotlin.effectsystem.resolving.parsers.InPlaceCallFunctorParser
import org.jetbrains.kotlin.effectsystem.resolving.parsers.ReturnsFunctorParser
import org.jetbrains.kotlin.effectsystem.structure.ESFunctor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class FunctorResolver {
    private val parsers: MutableList<FunctorParser> = mutableListOf(
            InPlaceCallFunctorParser(),
            ReturnsFunctorParser()
        )
    
    fun resolveFunctor(resolvedCall: ResolvedCall<*>): ESFunctor? {
        val resolutionResults = parsers.mapNotNull { it.tryParseFunctor(resolvedCall) }
        if (resolutionResults.isEmpty()) return null

        assert(resolutionResults.size == 1) {
            "Ambiguous functor resolution for call $resolvedCall. Candidates:\n" +
            resolutionResults.joinToString(separator = "\n")
        }

        return resolutionResults.single()
    }
}