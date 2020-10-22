// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// !LANGUAGE: +NewInference
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

import kotlin.collections.toList

fun <T: Number> foo(vararg values: T) = values.toList()

fun box(): String {
    val a = foo(1, 4.5)
    return "OK"
}
