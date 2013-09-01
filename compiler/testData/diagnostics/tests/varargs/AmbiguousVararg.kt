fun foo(vararg <!UNUSED_PARAMETER!>t<!> : String) = ""
fun foo(vararg <!UNUSED_PARAMETER!>t<!> : Int) = ""

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
}