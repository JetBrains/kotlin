// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM

data class A(val s: String) : Cloneable {
    fun externalClone(): A = clone() as A
}

fun box(): String {
    val a = A("OK")
    val b = a.externalClone()
    if (a != b) return "Fail equals"
    if (a === b) return "Fail identity"
    return b.s
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
