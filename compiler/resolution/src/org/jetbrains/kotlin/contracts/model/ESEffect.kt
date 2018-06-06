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

sealed class ESEffect {
    /**
     * Returns:
     *  - true, when presence of `this`-effect necessary implies presence of `other`-effect
     *  - false, when presence of `this`-effect necessary implies absence of `other`-effect
     *  - null, when presence of `this`-effect doesn't implies neither presence nor absence of `other`-effect
     */
    abstract fun isImplies(other: ESEffect): Boolean?
}

/**
 * Abstraction of some side-effect of a computation.
 *
 * SimpleEffect alone means that this effect will definitely be fired.
 */
abstract class SimpleEffect : ESEffect()

/**
 * Effect with condition attached to it.
 *
 * Has the same semantics as [org.jetbrains.kotlin.contracts.description.ConditionalEffectDeclaration]
 */
class ConditionalEffect(val condition: ESExpression, val simpleEffect: SimpleEffect) : ESEffect() {
    // Conservatively, always return null, indicating absence of information
    override fun isImplies(other: ESEffect): Boolean? = null
}



