/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

//class A<T>(val a: T) {
//    companion object
//}
//
//operator fun <T> A<T>.invoke(): T {
//    return a
//}


operator fun List.Companion.build(): List<Any> {
    return TODO()
}

operator fun Set.Companion.build(): Set<Any> {
    return TODO()
}

fun main() {
    val a: Set<Any> = [1, 2, 3]
    println(a)
}

