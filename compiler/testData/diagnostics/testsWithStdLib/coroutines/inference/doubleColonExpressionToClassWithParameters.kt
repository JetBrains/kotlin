// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNUSED_EXPRESSION

@file:OptIn(ExperimentalTypeInference::class)

package a.b

import kotlin.experimental.ExperimentalTypeInference

class BatchInfo1(val batchSize: Int)
class BatchInfo2<T>(val data: T)

object Obj

fun test1() {
    val a: Sequence<String> = sequence {
        val x = BatchInfo1::class
        val y = a.b.BatchInfo1::class
        val z = Obj::class

        val x1 = BatchInfo1::batchSize
        val y1 = a.b.BatchInfo1::class
    }
}

interface Scope<T> {
    fun yield(t: T) {}
}

fun <S> generate(g: Scope<S>.() -> Unit): S = TODO()

val test2 = generate {
    { yield("foo") }::class
}

val test3 = generate {
    ({ yield("foo") })::class
}
