fun test1(): Int {
    val inlineX = Inline(9)
    return inlineX.calcExt({(z: Int) -> z}, 25)
}

fun test2(): Int {
    val inlineX = Inline(9)
    return inlineX.calcExt2({Int.() -> this}, 25)
}

fun test3(): Int {
    val inlineX = Inline(9)
    return inlineX.doWork(InlineX(11))
}

fun test4(): Double {
    val inlineX = Inline(9)
    return inlineX.doWorkWithDouble(11.0)
}

fun test5(): Double {
    val inlineX = Inline(9)
    with(inlineX) {
        11.0.calcDouble{(a: Int, b: Double) -> a + b}
    }
    return inlineX.doWorkWithDouble(11.0)
}

fun box(): String {
    if (test1() != 25) return "test1: ${test1()}"
    if (test2() != 25) return "test2: ${test2()}"
    if (test3() != 20) return "test3: ${test3()}"
    if (test4() != 20.0) return "test4: ${test4()}"
    if (test5() != 20.0) return "test5: ${test5()}"

    return "OK"
}