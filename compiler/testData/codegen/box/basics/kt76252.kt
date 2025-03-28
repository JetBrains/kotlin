/*
 * Copyright 2010-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
import kotlin.test.*

value class ValueClass<T>(val value: T)

class ValueClassContainer<T>(val valueClass: ValueClass<T>)

fun foo(): Any {
    val f = ValueClassContainer(ValueClass("OK")).valueClass
    val s = f.value
    return s
}

fun box() = foo().toString()