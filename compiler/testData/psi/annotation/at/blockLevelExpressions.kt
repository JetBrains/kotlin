fun foo() {
    @ann0
    var x0 = foo0()

    @ann1
    x1 = foo1()

    @ann2
    x2 += foo2()

    for (i in 1..100) {
        @ann3
        x3 += foo3()
    }

    for (i in 1..100)
        // annotation is attached to `x4`
        @ann4
        x4 += foo4()

    a.filter {
        @ann5
        x5 += foo5()
    }

    @ann6
    x6 ?: x7 infix x9 + 10

    @ann7
    return 1

    @ann8
    x as Type
}
