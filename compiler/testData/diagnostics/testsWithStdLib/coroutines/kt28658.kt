// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

import kotlin.experimental.ExperimentalTypeInference

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>sequence<!> {
        val a: Array<Int> = arrayOf(1, 2, 3)
        val b = arrayOf(1, 2, 3)
    }
}

fun test2() = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>sequence<!> { arrayOf(1, 2, 3) }


class Foo<T>

fun <T> f1(f: Foo<T>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <T> f2(@BuilderInference f: Foo<T>.() -> Unit) {
}

fun test3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f1<!> {
        val a: Array<Int> = arrayOf(1, 2, 3)
    }

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>f2<!> {
        val a: Array<Int> = arrayOf(1, 2, 3)
    }
}

