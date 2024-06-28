/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.utils

private val IDENTITY: (Any?) -> Any? = { it }

@Suppress("UNCHECKED_CAST") fun <T> identity(): (T) -> T = IDENTITY as (T) -> T


private val ALWAYS_TRUE: (Any?) -> Boolean = { true }

fun <T> alwaysTrue(): (T) -> Boolean = ALWAYS_TRUE

private val ALWAYS_NULL: (Any?) -> Any? = { null }

@Suppress("UNCHECKED_CAST")
fun <T, R: Any> alwaysNull(): (T) -> R? = ALWAYS_NULL as (T) -> R?

val DO_NOTHING: (Any?) -> Unit = { }
val DO_NOTHING_2: (Any?, Any?) -> Unit = { _, _ -> }
val DO_NOTHING_3: (Any?, Any?, Any?) -> Unit = { _, _, _ -> }

fun <T> doNothing(): (T) -> Unit = DO_NOTHING

fun doNothing() {}

fun <Arg1, Bound, R> ((Arg1, Bound) -> R).bind(bound: Bound): ((Arg1) -> R) =
    { t1 -> this.invoke(t1, bound) }

fun <Arg1, Arg2, Bound, R> ((Arg1, Arg2, Bound) -> R).bind(bound: Bound): ((Arg1, Arg2) -> R) =
    { t1, t2 -> this.invoke(t1, t2, bound) }

fun <Arg1, Bound1, Bound2, R> ((Arg1, Bound1, Bound2) -> R).bind(bound1: Bound1, bound2: Bound2): ((Arg1) -> R) =
    { t1 -> this.invoke(t1, bound1, bound2) }
