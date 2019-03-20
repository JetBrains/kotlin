// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    take(<!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>nullable<!>() ?: <!IMPLICIT_NOTHING_AS_TYPE_PARAMETER!>nullable<!>() ?: "foo")
}

fun <T> nullable(): T? = TODO()
fun take(x: Any) {}