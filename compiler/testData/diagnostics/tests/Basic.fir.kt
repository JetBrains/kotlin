// !WITH_NEW_INFERENCE
fun foo(u : Unit) : Int = 1

fun test() : Int {
    foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val a : () -> Unit = {
        foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    }
    return 1 <!NONE_APPLICABLE!>-<!> "1"
}

class A() {
    val x : Int = foo1(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)
}

fun foo1() {}
