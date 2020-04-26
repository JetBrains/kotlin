// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class A {
    val s: Sequence<String> = sequence {
        val a = {}
        yield("OK")
    }
}

fun box(): String = A().s.single()

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: COROUTINES