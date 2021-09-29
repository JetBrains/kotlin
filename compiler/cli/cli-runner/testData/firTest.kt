/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner


// ВАЖНО Посмотреть где именно ILT превратится в шорт
//fun foo(s: Short) {
//    TODO(s.toString())
//}
//
//fun main() {
//    foo(2)
//}

//import kotlin.collections.Set.Companion.build
//import kotlin.collections.List.Companion.build
//
//fun foo(set: Set<Int>) {
////    TODO()
//}
//
//fun main() {
//    foo([1, 2, 3])
////    Set.build<Int>(2)
////    build<Int>(2)
//}

//import kotlin.collections.Set.Companion.build
//import kotlin.collections.CollectionLiteralBuilder
//import java.io.File



//class A {
//    companion object {
//        fun <T> build(size: Int, ianit: CollectionLiteralBuilder<A, T>.() -> Unit = {}): A {
//            return TODO()
//        }
//    }
//}
//
fun <T> Int.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}
//
//fun <T> Double.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
//    return TODO()
//}
//
fun <T> Set.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

fun <T> List.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
//    return object : CollectionLiteralBuilder<List<T>, T> {
//        private val buf = mutableSetOf<T>()
//
//        override fun add(element: T) {
//            buf.add(element)
//        }
//
//        override fun build(): List<T> {
//            return buf
//        }
//    }.apply(init).build()
}
//
interface A
open class B : A {
    companion object {

    }
}

fun <T> B.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
    TODO()
}
open class C : A {
    companion object {

    }
}

fun <T> C.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<C, T>.() -> Unit = {}): C {
    TODO()
}

//fun <T: A, Z> foo(a: T, b: Z) {
//
//}

fun <T> foo(c: Set<T>) {}
//fun foo(b: B) {}

//fun <T: A> a(): T = TODO()
//fun foo(s: Set<Int>, i: String, l: List<Int>) {
//}

//fun foo(a: Set<Int>) { }

//fun <T: Int> foo(a: T) { }

//fun <T> a(a: T): T = a

fun main() {
//    val a: Set<Int> = [1, 2, 3]
//    val a = [1, 2, 3]
//    val a = [1, 2, 3.0]
//    listOf(1, 2, 3)
//    listOf(listOf(1, 2, 3), setOf(4, 5, 6))
    foo([1, 2, 3])
//    foo(a())
//    foo([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
//    foo([listOf(), setOf()])
//    foo([1, 2, 3], "str", [4.2, 5, 6])
//    println(a.toString())
}
