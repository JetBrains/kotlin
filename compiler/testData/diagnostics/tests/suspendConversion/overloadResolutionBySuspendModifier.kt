// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: () -> Int) {}
fun foo(x: suspend () -> Int) {}

fun usualCall(): Int = 42
suspend fun suspendCall(): Int = 42

// it's important to have ambiguity in these cases to introduce overload resolution by suspend modifier in future
fun test1() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { usualCall() }
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { <!ILLEGAL_SUSPEND_FUNCTION_CALL!>suspendCall<!>() }
}

// candidate without suspend conversions is more specific
fun test2(f: () -> Int, g: suspend () -> Int) {
    foo(f)
    foo(g)
}