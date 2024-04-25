// FIR_IDENTICAL
// LANGUAGE: +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: () -> String, g: suspend () -> String, h: suspend () -> String) {}

fun test(f: () -> String, g: suspend () -> String) {
    foo(f, f, f)
    foo(f, { "str" }, f)
    foo(f, f, g)
    foo(f, g, g)
}
