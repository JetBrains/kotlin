// FILE: builders.kt

package builders

fun <T> List.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<List<T>, T>.() -> Unit = {}): List<T> {
    return TODO()
}

class B {
    companion object {
        fun <T> buildSeq(size: Int, init: SeqCollectionLiteralBuilder<B, T>.() -> Unit = {}): B {
            return TODO()
        }
    }
}

// FILE: main.kt

package main

import main.A.Companion.buildSeq
import main.buildSeq
import builders.buildSeq
import builders.B
import builders.B.Companion.buildSeq

fun <T> Set.Companion.buildSeq(size: Int, init: SeqCollectionLiteralBuilder<Set<T>, T>.() -> Unit = {}): Set<T> {
    return TODO()
}

class A {
    companion object {
        fun <T> buildSeq(size: Int, init: SeqCollectionLiteralBuilder<A, T>.() -> Unit = {}): A {
            return TODO()
        }
    }
}

fun f() {
    val a: Set<Int> = [1, 2, 3]
    val b: List<Int> = [1, 2, 3]
    val c: A = [1, 2, 3]
    val d: B = [1, 2, 3]
}

