// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: () -> Int) {}
fun foo(x: suspend () -> Int) {}

fun usualCall(): Int = 42
suspend fun suspendCall(): Int = 42

// candidate without suspend conversions is more specific
fun test2(f: () -> Int, g: suspend () -> Int) {
    foo(f)
    foo(g)
}

fun box(): String {
    test2({ 1 }, { 2 })
    return "OK"
}