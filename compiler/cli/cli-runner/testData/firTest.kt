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
//fun <T> Set.Companion.buildSeq(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
//    return TODO()
//}
////
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

//fun foo(s: Set<Int>, i: String, l: List<Int>) {
//}

fun foo(a: Set<Int>) {

}

fun main() {
//    val a = A.build<Int>(1) {
//        add(1)
//        add(2)
//        add(3)
//    }
//    val a: Set<Int> = [1, 2, 3]
//    listOf(1, 2, 3.0)
    foo([1, 2, 3])
//    foo([1, 2, 3], "str", [4.2, 5, 6])
//    println(a.toString())
}
