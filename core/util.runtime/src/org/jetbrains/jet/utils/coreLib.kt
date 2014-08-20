/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util

public fun <T: Any, R> T?.inn(then: (T) -> R, _else: R): R = if (this != null) then(this) else _else

public fun <T: Any> T?.sure(message: String): T = this ?: throw AssertionError(message)

fun <T> T.printAndReturn(message: String = ""): T {
    if (!message.isEmpty()) {
        println("$message:")
    }
    println(this)
    return this
}