// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR, WASM

// KT-61141: rethrows kotlin.Exception instead of java.lang.Exception
// IGNORE_BACKEND: NATIVE

fun test(f: () -> Unit) =
        try { f() } catch (e: Exception) { throw e }
