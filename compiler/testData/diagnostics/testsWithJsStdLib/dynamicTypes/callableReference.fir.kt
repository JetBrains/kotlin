fun foo() {
    val a: dynamic = Any()

    a.someFoo()
    a.someFoo(1, 2, 3)
    a.someFoo

    println(a::<!OVERLOAD_RESOLUTION_AMBIGUITY!>someFoo<!>)

    with(a) {
        println(::<!OVERLOAD_RESOLUTION_AMBIGUITY!>someFoo<!>)
        someFoo()
        someFoo(1, 2, 3)
        someFoo
    }
}
