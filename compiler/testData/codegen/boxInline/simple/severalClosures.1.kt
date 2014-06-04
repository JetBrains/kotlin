fun test1(): Double {
    val inlineX = Inline()
    return inlineX.foo1({(z: Int) -> z}, 25, {(z: Double) -> z}, 11.5)
}

fun test1WithCaptured(): Double {
    val inlineX = Inline()
    var d = 0.0;
    return inlineX.foo1({(z: Int) -> d = 1.0; z}, 25, {(z: Double) -> z + d}, 11.5)
}

fun test2(): Double {
    val inlineX = Inline()
    return inlineX.foo2({(z: Int, p: Int) -> z + p}, 25, {(x: Double, y: Int, z: Int) -> z + x + y}, 11.5, 2)
}

fun box(): String {
    if (test1() != 36.5) return "test1: ${test1()}"
    if (test1WithCaptured() != 37.5) return "test1WithCaptured: ${test1WithCaptured()}"
    if (test2() != 65.5) return "test2: ${test2()}"

    return "OK"
}