// LANGUAGE: +SuspendConversion
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(f: suspend () -> String) {}
fun foo2(f: suspend (Int) -> String) {}
fun foo3(f: suspend () -> Unit) {}

fun test(
    f0: suspend () -> String,
    f1: () -> String,
    f2: (Int) -> String,
    f3: () -> Unit,
) {
    foo1 { "str" }
    foo1(f0)

    foo1(f1)
    foo2(f2)
    foo3(f3)

    foo1(<!TYPE_MISMATCH!>f2<!>)
    foo1(<!TYPE_MISMATCH!>f3<!>)
}
