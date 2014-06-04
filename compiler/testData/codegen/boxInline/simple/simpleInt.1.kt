fun test0Param(): Int {
    val inlineX = Inline(10)
    return inlineX.foo({() -> 1})
}

fun test1Param(): Int {
    val inlineX = Inline(10)
    return inlineX.foo11({(z: Int) -> z})
}

fun test1ParamCaptured(): Int {
    val s = 100
    val inlineX = Inline(10)
    return inlineX.foo11({(z: Int) -> s})
}

fun test1ParamMissed() : Int {
    val inlineX = Inline(10)
    return inlineX.foo11({(z: Int) -> 111})
}

fun test1ParamFromCallContext() : Int {
    val inlineX = Inline(1000)
    return inlineX.fooRes({(z: Int) -> z})
}

fun test2Params() : Int {
    val inlineX = Inline(1000)
    return inlineX.fooRes2({(y: Int, z: Int) -> 2 * y + 3 * z})
}

fun test2ParamsWithCaptured() : Int {
    val inlineX = Inline(1000)
    val s = 9
    var t = 1
    return inlineX.fooRes2({(y: Int, z: Int) -> 2 * s + t})
}

fun box(): String {
    if (test0Param() != 1) return "test0Param: ${test0Param()}"
    if (test1Param() != 11) return "test1Param: ${test1Param()}"
    if (test1ParamCaptured() != 100) return "test1ParamCaptured: ${test1ParamCaptured()}"
    if (test1ParamMissed() != 111) return "test1ParamMissed: ${test1ParamMissed()}"
    if (test1ParamFromCallContext() != 1000) return "test1ParamFromCallContext: ${test1ParamFromCallContext()}"
    if (test2Params() != 35) return "test2Params: ${test2Params()}"
    if (test2ParamsWithCaptured() != 19) return "test2ParamsWithCaptured: ${test2ParamsWithCaptured()}"
    return "OK"
}