// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.Experimental
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

// ISSUE: KT-35684

import kotlin.experimental.ExperimentalTypeInference

fun test() {
    sequence {
        yield(materialize())
        yield(materialize<Int>())
    }
}

@UseExperimental(ExperimentalTypeInference::class)
fun <U> sequence(@BuilderInference block: suspend Inv<U>.() -> Unit) {}

interface Inv<T> {
    fun yield(element: T)
}

fun <K> materialize(): Inv<K> = TODO()

fun box(): String = "OK"