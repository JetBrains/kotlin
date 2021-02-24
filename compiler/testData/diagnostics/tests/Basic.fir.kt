// !WITH_NEW_INFERENCE
fun foo(u : Unit) : Int = 1

fun test() : Int {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    val a : () -> Unit = {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    }
    return 1 <!NONE_APPLICABLE!>-<!> "1"
}

class A() {
    val x : Int = <!INAPPLICABLE_CANDIDATE!>foo1<!>(<!UNRESOLVED_REFERENCE!>xx<!>)
}

fun foo1() {}
