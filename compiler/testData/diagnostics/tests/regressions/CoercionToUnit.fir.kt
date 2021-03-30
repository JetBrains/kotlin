fun foo(u : Unit) : Int = 1

fun test() : Int {
    foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    val a : () -> Unit = {
        foo(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    }
    return 1
}
