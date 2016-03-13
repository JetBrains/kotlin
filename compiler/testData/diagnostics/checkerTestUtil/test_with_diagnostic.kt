interface Pa<X, Y>

fun foo(<!UNUSED_PARAMETER(IGNORE)!>u<!> : Unit) : Int = 1
fun bas(<!UNUSED_PARAMETER!>u<!> : Pa<Int, String>) = 3

fun test() : Int {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH(IGNORE; Unit)!>1<!>)
    val <!UNUSED_VARIABLE!>a<!> : () -> Unit = {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH(integer; IGNORE)!>1<!>)
    bas(<!CONSTANT_EXPECTED_TYPE_MISMATCH(integer; Pa<Int, String>)!>1<!>)
}
return 1 <!NONE_APPLICABLE!>-<!> "1"
}

class A() {
    val x : Int = <!TYPE_MISMATCH!>foo1(<!UNRESOLVED_REFERENCE, TOO_MANY_ARGUMENTS!>xx<!>)<!>
}

fun foo1() {}