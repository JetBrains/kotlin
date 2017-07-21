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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.effectsystem.impls.and
import org.jetbrains.kotlin.effectsystem.impls.not
import org.jetbrains.kotlin.effectsystem.impls.or
import org.jetbrains.kotlin.effectsystem.structure.ESBooleanExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils.getFqNameSafe

/** Annotations names */
val CONDITION_JOINING_ANNOTATION = FqName("kotlin.internal.JoinConditions")

val EQUALS_CONDITION = FqName("kotlin.internal.Equals")
val IS_INSTANCE_CONDITION = FqName("kotlin.internal.IsInstance")
val NOT_CONDITION = FqName("kotlin.internal.Not")

val CALLS_EFFECT = FqName("kotlin.internal.CalledInPlace")
val RETURNS_EFFECT = FqName("kotlin.internal.Returns")

fun ClassDescriptor?.fqNameEquals(other: FqName) =
        this != null && this.name == other.shortName() && other == getFqNameSafe(this)



/** Effects enum values  */
enum class EffectsConditionsJoiners {
    ALL {
        override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                conditions.reduce { acc, expr -> acc.and(expr) }
    },

    NONE {
        override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                conditions.map { it.not() }.reduce { acc, expr -> acc.and(expr) }
    },

    ANY {
        override fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression =
                conditions.reduce { acc, expr -> acc.or(expr) }
    };

    abstract fun join(conditions: List<ESBooleanExpression>): ESBooleanExpression

    companion object {
        fun safeValueOf(name: String?) = if (name == null) ALL else values().find { it.name == name }
    }
}

enum class EffectsConstantValues {
    TRUE,
    FALSE,
    NULL,
    NOT_NULL,
    UNKNOWN;

    companion object {
        fun safeValueOf(name: String?) = if (name == null) UNKNOWN else values().find { it.name == name }
    }
}