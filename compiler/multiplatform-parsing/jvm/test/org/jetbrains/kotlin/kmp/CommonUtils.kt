/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

val kotlinCodeSample = """fun main() {
    println("Hello, World!")
}

class C(val x: Int)

/**
 * @param [C.x] Some parameter.
 * @return [Exception]
 */
fun test(p: String) {
    val badCharacter = ^
    throw Exception()
}"""