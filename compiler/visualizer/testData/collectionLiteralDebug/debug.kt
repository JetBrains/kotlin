package org.jetbrains.kotlin.runner

import kotlin.collections.Set.Companion.build
import kotlin.collections.CollectionLiteralBuilder

class A {
    companion object {
        fun <T> build(size: Int, ianit: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
            return TODO()
        }
    }
}

//fun <T> Set.Companion.build(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
//    return TODO()
//}

fun <T> List.Companion.build(size: Int, init: CollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return object : CollectionLiteralBuilder<Set<T>, T> {
        private val buf = mutableSetOf<T>()
        override fun add(element: T) {
            buf.add(element)
        }
        override fun build(): Set<T> {
            return buf
        }
    }.apply(init).build()
}

fun main() {
//    listOf(1, 2 ,3, 0.3)

//    val a = List.build<Int>(3) {
//        add(1)
//        add(1)
//        add(2)
//        add(3)
//    }
    val a = [1, 2, 3.0]
    println(a.toString())
}