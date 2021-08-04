/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

//open class A<T>(val a: T) {
//    companion object
//}
//
//class B<T>(a: T) : A<T>(a) {
//
//}

//operator fun <T> A<T>.invoke(): T {
//    return a
//}


//operator fun List.Companion.build(): List<Any> {
//    return TODO()
//}
//
//operator fun Set.Companion.build(): Set<Any> {
//    return TODO()
//}


// ВАЖНО Посмотреть где именно ILT превратится в шорт
//fun foo(s: Short) {
//    TODO(s.toString())
//}
//
//fun main() {
//    foo(2)
//}

import kotlin.collections.Set.Companion.build
import kotlin.collections.List.Companion.build

fun foo(set: Set<Int>) {
//    TODO()
}

fun main() {
    foo([1, 2, 3])
//    Set.build<Int>(2)
//    build<Int>(2)
}
