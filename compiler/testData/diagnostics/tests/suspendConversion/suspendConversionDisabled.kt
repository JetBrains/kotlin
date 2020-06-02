// !LANGUAGE: -SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(f: suspend () -> String) {}
fun foo2(f: suspend (Int) -> String) {}
fun foo3(f: suspend () -> Unit) {}

fun bar(): String = ""

fun test(
    f0: suspend () -> String,
    f1: () -> String,
    f2: (Int) -> String,
    f3: () -> Unit,
) {
    foo1 { "str" }
    foo1(f0)

    foo1(<!UNSUPPORTED_FEATURE!>f1<!>)
    foo2(<!UNSUPPORTED_FEATURE!>f2<!>)
    foo3(<!UNSUPPORTED_FEATURE!>f3<!>)

    foo1(::bar)

    foo1(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>f2<!>)
    foo1(<!TYPE_MISMATCH, UNSUPPORTED_FEATURE!>f3<!>)
}
