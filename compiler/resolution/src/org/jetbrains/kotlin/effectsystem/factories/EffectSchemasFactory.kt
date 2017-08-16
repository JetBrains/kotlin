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

package org.jetbrains.kotlin.effectsystem.factories

import org.jetbrains.kotlin.effectsystem.effects.ESReturns
import org.jetbrains.kotlin.effectsystem.impls.ESBooleanConstant
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.effectsystem.impls.EffectSchemaImpl
import org.jetbrains.kotlin.effectsystem.structure.*

/**
 * Creates more specific schemas for some particular cases (e.g. explicitly specifying
 * that schema for 'false' never returns 'true', etc.)
 */
fun schemaForConstant(constant: ESConstant): EffectSchema {
    if (constant is ESBooleanConstant) {
        return boundSchemaFromClauses(
                createClause(true.lift(), ESReturns(constant)),
                createClause(false.lift(), ESReturns(constant.negated()))
        )
    }

    if (constant == null.lift()) {
        return boundSchemaFromClauses(
                createClause(true.lift(), ESReturns(constant)),
                createClause(false.lift(), ESReturns(NOT_NULL_CONSTANT))
        )
    }

    if (constant == NOT_NULL_CONSTANT) {
        return boundSchemaFromClauses(
                createClause(true.lift(), ESReturns(constant)),
                createClause(false.lift(), ESReturns(null.lift()))
        )
    }

    return pureSchema(constant)
}

fun pureSchema(value: ESValue): EffectSchema = boundSchemaFromClauses(createUnconditionalClause(ESReturns(value)))

fun singleClauseSchema(premise: ESBooleanExpression, effect: ESEffect, variables: List<ESVariable>) = schemaFromClauses(listOf(createClause(premise, effect)), variables)

fun schemaFromClauses(clauses: List<ESClause>, params: List<ESVariable>): EffectSchema = EffectSchemaImpl(clauses, params)

fun boundSchemaFromClauses(vararg clauses: ESClause): EffectSchema = schemaFromClauses(clauses.asList(), listOf())
fun boundSchemaFromClauses(clauses: List<ESClause>): EffectSchema = schemaFromClauses(clauses, listOf())