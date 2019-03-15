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

/**
 * Applies [operation] to [first] and [second] if both not-null, otherwise returns null
 */
internal fun <F, S, R> applyIfBothNotNull(first: F?, second: S?, operation: (F, S) -> R): R? =
    if (first == null || second == null) null else operation(first, second)

/**
 * If both [first] and [second] are null, then return null
 * If only one of [first] and [second] is null, then return other one
 * Otherwise, return result of [operation]
 */
internal fun <F : R, S : R, R> applyWithDefault(first: F?, second: S?, operation: (F, S) -> R): R? = when {
    first == null && second == null -> null
    first == null -> second
    second == null -> first
    else -> operation(first, second)
}
