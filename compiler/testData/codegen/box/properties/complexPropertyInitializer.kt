// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: COROUTINES
// WITH_RUNTIME

class A {
    val s: Sequence<String> = sequence {
        val a = {}
        yield("OK")
    }
}

fun box(): String = A().s.single()