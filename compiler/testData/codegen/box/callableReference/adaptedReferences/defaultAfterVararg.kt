// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType

fun foo(vararg a: String, result: String = "OK"): String =
        if (a.size == 0) result else "Fail"

fun call(f: () -> String): String = f()

fun box(): String {
    return call(::foo)
}
