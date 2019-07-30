package foo

fun test_1(gen: CommonGen<A>) {
    gen.a.commonFun()
    gen.a.platformFun()
}

fun test_2(gen: CommonGen<AImpl>) {
    gen.a.commonFun()
    gen.a.platformFun()
}

fun test_3(gen: CommonGen<*>) {
    gen.a.commonFun()
    gen.a.platformFun()
}

fun test_4(gen: CommonGen<out A>) {
    gen.a.commonFun()
    gen.a.platformFun()
}

fun test_5(gen: CommonGen<out AImpl>) {
    gen.a.commonFun()
    gen.a.platformFun()
}

fun test_6() {
    takeList(getList())
}