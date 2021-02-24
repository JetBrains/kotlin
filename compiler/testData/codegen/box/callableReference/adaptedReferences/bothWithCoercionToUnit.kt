// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType

fun foo(s: String = "kotlin", vararg t: String): Boolean {
    if (s != "kotlin") throw AssertionError(s)
    if (t.size != 0) throw AssertionError(t.size.toString())
    return true
}

fun bar(f: () -> Unit) {
    f()
}

fun box(): String {
    bar(::foo)
    return "OK"
}
