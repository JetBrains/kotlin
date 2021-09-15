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
//        fun <T> build(size: Int, ianit: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
//            return TODO()
//        }
//    }
//}
//
fun <T> Int.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Int, T>.() -> Unit = {}) : Int {
    return TODO()
}

fun <T> Double.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Double, T>.() -> Unit = {}) : Double {
    return TODO()
}

//fun <T> Set.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
//    return TODO()
//}
//
//fun <T> List.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
//    return TODO()
////    return object : CollectionLiteralBuilder<List<T>, T> {
////        private val buf = mutableSetOf<T>()
////
////        override fun add(element: T) {
////            buf.add(element)
////        }
////
////        override fun build(): List<T> {
////            return buf
////        }
////    }.apply(init).build()
//}

interface A
class B : A {
    companion object {

    }
}

fun <T> B.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
    TODO()
}
class C : A {
    companion object {

    }
}

fun <T> C.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<С, T>.() -> Unit = {}): C {
    TODO()
}

fun <T: C> foo(a: T) {

}

//fun foo(s: Set<Int>, i: String, l: List<Int>) {
//}

//fun foo(a: Set<Int>) { }

//fun <T: Int> foo(a: T) { }

//fun <T> a(a: T): T = a

fun main() {
//    val a = A.build<Int>(1) {
//        add(1)
//        add(2)
//        add(3)
//    }
//    val a: Set<Int> = [1, 2, 3]
//    val a = [1, 2, 3]
//    val a = [1, 2, 3.0]
//    listOf(1, 2, 3)
//    listOf(listOf(1, 2, 3), setOf(4, 5, 6))
//    foo([1, 2, 3])
    foo([1, 2, 3])
//    foo([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
//    foo([listOf(), setOf()])
//    foo([1, 2, 3], "str", [4.2, 5, 6])
//    println(a.toString())
}
