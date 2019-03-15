// !WITH_NEW_INFERENCE
fun foo(<!UNUSED_PARAMETER!>u<!> : Unit) : Int = 1

fun test() : Int {
    foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    val <!UNUSED_VARIABLE!>a<!> : () -> Unit = {
        foo(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>)
    }
    return 1 <!NONE_APPLICABLE!>-<!> "1"
}

class A() {
    val x : Int = <!NI;TYPE_MISMATCH, TYPE_MISMATCH!>foo1(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)<!>
}

fun foo1() {}
