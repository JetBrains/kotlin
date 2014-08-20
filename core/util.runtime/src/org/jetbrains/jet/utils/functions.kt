/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.utils

private val IDENTITY: Function1<Any?, Any?> = { it }

suppress("UNCHECKED_CAST")
public fun <T> identity(): Function1<T, T> = IDENTITY as Function1<T, T>


private val ALWAYS_TRUE: Function1<Any?, Boolean> = { true }

public fun <T> alwaysTrue(): Function1<T, Boolean> = ALWAYS_TRUE
