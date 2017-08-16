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

package org.jetbrains.kotlin.effectsystem.impls

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.effects.ESThrows
import org.jetbrains.kotlin.effectsystem.factories.lift
import org.jetbrains.kotlin.effectsystem.functors.AndFunctor
import org.jetbrains.kotlin.effectsystem.functors.NotFunctor
import org.jetbrains.kotlin.effectsystem.functors.OrFunctor
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.effectsystem.structure.ESEffect

// Premise extensions
fun ESBooleanExpression.and(otherExpression: ESBooleanExpression): ESBooleanExpression = ESAnd(this, otherExpression, AndFunctor())

fun ESBooleanExpression.or(otherExpression: ESBooleanExpression): ESBooleanExpression = ESOr(this, otherExpression, OrFunctor())

fun ESBooleanExpression.not(): ESBooleanExpression = ESNot(this, NotFunctor())

fun ESEffect.isSequential(): Boolean = this is ESReturns || this is ESThrows