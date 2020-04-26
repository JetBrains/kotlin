// IGNORE_BACKEND: JS_IR

class Outer(val x: Any) {
    inner class Inner(
        val fn: () -> String
    ) {
        constructor(
            unused: Int,
            fn: () -> String = { x.toString() }
        ) : this(fn)
    }
}

fun box() = Outer("OK").Inner(1).fn()

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: FAILS_IN_JS_IR
