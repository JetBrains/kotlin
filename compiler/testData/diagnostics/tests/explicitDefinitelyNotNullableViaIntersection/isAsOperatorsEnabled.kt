// FIR_IDENTICAL
// SKIP_TXT
// !LANGUAGE: +DefinitelyNotNullTypeParameters

fun Any.bar() {}
fun Boolean.baz() {}

var x: Int = 0

inline fun <reified T> foo(v: Any?): T {
    if (x > 0) 1 else v as T <!SYNTAX!>& Any<!>
    if (x > 1) 2 else v as? T <!SYNTAX!>& Any<!>
    if (x > 2) 3 else v is T <!SYNTAX!>& Any<!>
    if (x > 3) 4 else v !is T <!SYNTAX!>& Any<!>

    return v as T <!SYNTAX!>& Any<!>
}
