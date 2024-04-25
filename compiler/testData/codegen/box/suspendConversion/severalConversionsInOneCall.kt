// LANGUAGE: +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER
// IGNORE_BACKEND: JVM

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
