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

package org.jetbrains.kotlin.contracts.model.functors

import org.jetbrains.kotlin.contracts.model.ESEffect
import org.jetbrains.kotlin.contracts.model.Functor
import org.jetbrains.kotlin.contracts.model.Computation
import org.jetbrains.kotlin.contracts.model.visitors.Reducer

/**
 * Abstract implementation of Functor with some routine house-holding
 * automatically performed. *
 */
abstract class AbstractReducingFunctor : Functor {
    private val reducer = Reducer()

    override fun invokeWithArguments(arguments: List<Computation>): List<ESEffect> = reducer.reduceEffects(doInvocation(arguments))

    protected abstract fun doInvocation(arguments: List<Computation>): List<ESEffect>
}