fun foo() {

    x2.filter c @ { // should be no space after c
        return 2
    }

    x3.filter @ { // no label identifier
        return 3
    }

    loop2 @ for (i in 1..100) { // should be no space after loop2
        return@ loop2 5
        return @ loop2 7
        return
        @loop2 4
    }

    @ while (1) {
        return 123
    }

    label2 @ fun foo() {}  // should be no space after label2

    1 + label3@ 3 + 4

    l1 @ foo bar l2 @ baz // binary expression with extra spaces
    foo l3 @ bar baz // binary expression with `@ bar` parsed as wrong label

    foo@ bar baz // binary expression labeled `bar` and return as second arg

    return @ 1
}
