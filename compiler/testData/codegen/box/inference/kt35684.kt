// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// WITH_RUNTIME

// ISSUE: KT-35684

import kotlin.experimental.ExperimentalTypeInference

fun test() {
    sequence {
        yield(materialize())
        yield(materialize<Int>())
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <U> sequence(@BuilderInference block: suspend Inv<U>.() -> Unit) {}

interface Inv<T> {
    fun yield(element: T)
}

fun <K> materialize(): Inv<K> = TODO()

fun box(): String = "OK"
