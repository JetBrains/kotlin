// FIR_IDENTICAL
fun foo(vararg t : String) = ""
fun foo(vararg t : Int) = ""

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
}
