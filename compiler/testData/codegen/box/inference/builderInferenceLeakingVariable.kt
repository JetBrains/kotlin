// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// Issues: KT-33542, KT-33544
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR
// !LANGUAGE: +NewInference

import kotlin.experimental.ExperimentalTypeInference

interface In<in E> {
    suspend fun send(element: E)
}

class InImpl<E>(val block: suspend In<E>.() -> Unit) : In<E> {
    override suspend fun send(element: E) {}
}

@OptIn(ExperimentalTypeInference::class)
public fun <T> builder(@BuilderInference block: suspend In<T>.() -> Unit) {
    InImpl(block)
}

fun test33542() {
    builder {
        send(run {
            15
        })
    }
}

fun test33544(){
    builder {
        send(run {
            let { 0 } ?: 1
            0
        })
    }
}

fun box() = "OK"
