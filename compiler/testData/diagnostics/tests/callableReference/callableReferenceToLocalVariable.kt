// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

fun a() {
    val x = 10
    foo(::<!UNSUPPORTED!>x<!>)
}

fun foo(a: Any) {}