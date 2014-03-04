fun testAll(): String {
    val inlineX = InlineAll()

    return inlineX.inline({(a1: Int, a2: Double, a3: Double, a4: String, a5: Long) ->
                              "" + a1 + a2 + a3 + a4 + a5},
                          1, 12.0, 13.0, "14", 15)
}

fun testAllWithCapturedVal(): String {
    val inlineX = InlineAll()

    val c1 = 21
    val c2 = 22.0
    val c3 = 23.0
    val c4 = "24"
    val c5 = 25.toLong()
    val c6 = 'H'
    val c7 = 26.toByte()
    val c8 = 27.toShort()
    val c9 = 28.toFloat()

    return inlineX.inline({(a1: Int, a2: Double, a3: Double, a4: String, a5: Long) ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.0, 13.0, "14", 15)
}

fun testAllWithCapturedVar(): String {
    val inlineX = InlineAll()

    var c1 = 21
    var c2 = 22.0
    var c3 = 23.0
    var c4 = "24"
    var c5 = 25.toLong()
    var c6 = 'H'
    var c7 = 26.toByte()
    var c8 = 27.toShort()
    val c9 = 28.toFloat()

    return inlineX.inline({(a1: Int, a2: Double, a3: Double, a4: String, a5: Long) ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.0, 13.0, "14", 15)
}

fun testAllWithCapturedValAndVar(): String {
    val inlineX = InlineAll()

    var c1 = 21
    var c2 = 22.0
    val c3 = 23.0
    val c4 = "24"
    var c5 = 25.toLong()
    val c6 = 'H'
    var c7 = 26.toByte()
    var c8 = 27.toShort()
    val c9 = 28.toFloat()

    return inlineX.inline({(a1: Int, a2: Double, a3: Double, a4: String, a5: Long) ->
                              "" + a1 + a2 + a3 + a4 + a5 + c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9},
                          1, 12.0, 13.0, "14", 15)
}


fun box(): String {
    if (testAll() != "112.013.01415") return "testAll: ${testAll()}"
    if (testAllWithCapturedVal() != "112.013.014152122.023.02425H262728.0") return "testAllWithCapturedVal: ${testAllWithCapturedVal()}"
    if (testAllWithCapturedVar() != "112.013.014152122.023.02425H262728.0") return "testAllWithCapturedVar: ${testAllWithCapturedVar()}"
    if (testAllWithCapturedValAndVar() != "112.013.014152122.023.02425H262728.0") return "testAllWithCapturedVal: ${testAllWithCapturedValAndVar()}"
    return "OK"
}