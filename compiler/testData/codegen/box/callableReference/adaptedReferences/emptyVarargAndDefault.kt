// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND_FIR: JVM_IR

fun foo(x: String = "O", vararg y: String): String =
        if (y.size == 0) x + "K" else "Fail"

fun call(f: () -> String): String = f()

fun box(): String {
    return call(::foo)
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
