package foo

fun test_a(a: A) {
    a.commonFun()
    a.platformFun()

    a.b.commonFunB()
    a.b.platformFunB()

    a.bFun().commonFunB()
    a.bFun().platformFunB()
}

fun test_c(c: Common) {
    c.a.commonFun()
    c.a.platformFun()

    c.aFun().commonFun()
    c.aFun().platformFun()

    c.a.b.commonFunB()
    c.a.b.platformFunB()

    c.aFun().b.commonFunB()
    c.aFun().b.platformFunB()

    c.a.bFun().commonFunB()
    c.a.bFun().platformFunB()

    c.aFun().bFun().commonFunB()
    c.aFun().bFun().platformFunB()
}

