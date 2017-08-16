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
 * An abstraction of effect-generating nature of some operation.
 *
 * [ESFunctor] roughly corresponds to some call, but it cares only
 * about effects of this call, so instead of value arguments, its [apply]
 * method takes just description of effects of each argument
 * (represented by [EffectSchema]), and produces effects of the whole call
 * (instead of some value, compared to usual calls).
 *
 * Implementors should never produce non-conservative effects.
 * However, they are not obliged to produce most precise description
 * of effects, though they are encouraged to do so.
 */

interface ESFunctor {
    fun apply(arguments: List<EffectSchema>): EffectSchema?
}

