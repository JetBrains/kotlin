// !WITH_NEW_INFERENCE
fun foo(u : Unit) : Int = 1

fun test() : Int {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    val a : () -> Unit = {
        <!INAPPLICABLE_CANDIDATE!>foo<!>(1)
    }

    val b : (Int) -> Unit = { i ->
        i
    }

    return 1 <!NONE_APPLICABLE!>-<!> "1"
}

class A() {
    val x : Int = <!INITIALIZER_TYPE_MISMATCH!>foo1(<!TOO_MANY_ARGUMENTS, UNRESOLVED_REFERENCE!>xx<!>)<!>
}

fun foo1() {}
