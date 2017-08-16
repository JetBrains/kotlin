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

package org.jetbrains.kotlin.effectsystem.structure


/**
 * Set of clauses, describing possible effects of some computation and
 * corresponding conditions.
 *
 * Clients of [EffectSchema] should be aware that this description can be
 * incomplete, meaning that if [clauses] doesn't mention some effect,
 * it is interpreted as an *absence of information* about that effect.
 */
interface EffectSchema : ESFunctor {
    val clauses: List<ESClause>
}