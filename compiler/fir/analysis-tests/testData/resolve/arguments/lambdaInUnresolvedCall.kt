fun <R> materialize(): R = null!!

fun test_1() {
    <!UNRESOLVED_REFERENCE!>myRun<!> {
        val x = 1
        x * 2
    }
}

fun test_2() {
    <!UNRESOLVED_REFERENCE!>myRun<!> {
        materialize()
    }
}