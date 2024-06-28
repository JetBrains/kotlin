fun foo() {
    val a: dynamic = Any()
    println(a[0])
    println(<!WRONG_OPERATION_WITH_DYNAMIC!>a[0, 1]<!>)

    a[0] = 23
    <!WRONG_OPERATION_WITH_DYNAMIC!>a[0, 1] = 42<!>
}
