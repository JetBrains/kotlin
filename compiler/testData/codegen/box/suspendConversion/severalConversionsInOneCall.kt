// FIR_IDENTICAL
// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM

fun foo(f: () -> String, g: suspend () -> String, h: suspend () -> String) {}

fun test(f: () -> String, g: suspend () -> String) {
    foo(f, f, f)
    foo(f, { "str" }, f)
    foo(f, f, g)
    foo(f, g, g)
}

fun box(): String {
    test({ "1" }, { "2" })
    return "OK"
}
