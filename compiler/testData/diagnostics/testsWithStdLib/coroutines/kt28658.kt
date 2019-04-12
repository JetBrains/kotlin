// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.experimental.ExperimentalTypeInference

fun test1() {
    sequence {
        val a: Array<Int> = arrayOf(1, 2, 3)
        val b = arrayOf(1, 2, 3)
    }
}

fun test2() = sequence { arrayOf(1, 2, 3) }


class Foo<T>

fun <T> f1(f: Foo<T>.() -> Unit) {}

@UseExperimental(ExperimentalTypeInference::class)
fun <T> f2(@BuilderInference f: Foo<T>.() -> Unit) {
}

fun test3() {
    f1 {
        val a: Array<Int> = arrayOf(1, 2, 3)
    }

    f2 {
        val a: Array<Int> = arrayOf(1, 2, 3)
    }
}

