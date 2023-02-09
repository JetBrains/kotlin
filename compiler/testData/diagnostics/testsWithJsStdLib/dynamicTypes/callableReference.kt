fun foo() {
    val a: dynamic = Any()

    a.someFoo()
    a.someFoo(1, 2, 3)
    a.someFoo

    println(<!WRONG_OPERATION_WITH_DYNAMIC!>a::someFoo<!>)

    with(a) {
        println(<!WRONG_OPERATION_WITH_DYNAMIC!>::someFoo<!>)
        someFoo()
        someFoo(1, 2, 3)
        someFoo
    }
}
