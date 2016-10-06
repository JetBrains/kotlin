fun foo() {
    @ann0
    var x0 = foo0()

    @ann1
    // comment
    /* comment */
    x1 = foo1()

    // many empty new lines
    @ann2


    x2 += foo2()

    @ann21 @ann22

    @ann23
    x22 += foo22()

    for (i in 1..100) {
        @ann3
        x3 += foo3()
    }

    for (i in 1..100)
        @ann4
        x4 += foo4()

    if (1 > 2)
        @ann41
        x41 += foo41()

    if (3 > 4)
        @ann42
        x42 += foo42()
    else
        @ann43
        x43 += foo43()

    while (true)
        @ann44
        x44 += foo44()

    do
        @ann
        x45 += foo45()
    while (true)

    when (1) {
        1 ->
            @ann46
            x46 += foo46()
    }

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
