fun foo(<!UNUSED_PARAMETER!>u<!> : Unit) : Int = 1

fun test() : Int {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>a<!> : () -> Unit = {
        foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    }
    return 1
}
