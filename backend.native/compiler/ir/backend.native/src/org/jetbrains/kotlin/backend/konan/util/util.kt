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

package org.jetbrains.kotlin.backend.konan.util

import kotlin.system.measureTimeMillis

fun printMillisec(message: String, body: () -> Unit) {
    val msec = measureTimeMillis{
        body()
    }
    println("$message: $msec msec")
}

fun profile(message: String, body: () -> Unit) = profileIf(
    System.getProperty("konan.profile")?.equals("true") ?: false,
    message, body)

fun profileIf(condition: Boolean, message: String, body: () -> Unit) =
    if (condition) printMillisec(message, body) else body()

fun nTabs(amount: Int): String {
    return String.format("%1$-${(amount+1)*4}s", "") 
}

fun <T> Collection<T>.atMostOne(): T? {
    return when (this.size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

inline fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean): T? = this.filter(predicate).atMostOne()

fun <T: Any> T.onlyIf(condition: T.()->Boolean, then: (T)->Unit): T {
    if (this.condition()) then(this)
    return this
}

