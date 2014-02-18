fun test0Param(): String {
    val inlineX = InlineString("10")
    return inlineX.foo({() -> "1"})
}

fun test1Param(): String {
    val inlineX = InlineString("10")
    return inlineX.foo11({(z: String) -> z})
}

fun test1ParamCaptured(): String {
    val s = "100"
    val inlineX = InlineString("10")
    return inlineX.foo11({(z: String) -> s})
}

fun test1ParamMissed() : String {
    val inlineX = InlineString("10")
    return inlineX.foo11({(z: String) -> "111"})
}

fun test1ParamFromCallContext() : String {
    val inlineX = InlineString("1000")
    return inlineX.fooRes({(z: String) -> z})
}

fun test2Params() : String {
    val inlineX = InlineString("1000")
    return inlineX.fooRes2({(y: String, z: String) -> y + "0" + z})
}

fun test2ParamsWithCaptured() : String {
    val inlineX = InlineString("1000")
    val s = "9"
    var t = "1"
    return inlineX.fooRes2({(y: String, z: String) -> s + t})
}

fun box(): String {
    if (test0Param() != "1") return "test0Param: ${test0Param()}"
    if (test1Param() != "11") return "test1Param: ${test1Param()}"
    if (test1ParamCaptured() != "100") return "test1ParamCaptured: ${test1ParamCaptured()}"
    if (test1ParamMissed() != "111") return "test1ParamMissed: ${test1ParamMissed()}"
    if (test1ParamFromCallContext() != "1000") return "test1ParamFromCallContext: ${test1ParamFromCallContext()}"
    if (test2Params() != "1011") return "test2Params: ${test2Params()}"
    if (test2ParamsWithCaptured() != "91") return "test2ParamsWithCaptured: ${test2ParamsWithCaptured()}"

    return "OK"
}