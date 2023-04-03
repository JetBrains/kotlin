fun foo() {
    val a: dynamic = Any()
    println(a in setOf(1, 2))
    println(<!WRONG_OPERATION_WITH_DYNAMIC!>1 in a<!>)
    println(<!WRONG_OPERATION_WITH_DYNAMIC!>1 !in a<!>)
    when (2) {
        <!WRONG_OPERATION_WITH_DYNAMIC!>in a<!> -> println("ok")
    }
    when (3) {
        <!WRONG_OPERATION_WITH_DYNAMIC!>!in a<!> -> println("ok")
    }
}
