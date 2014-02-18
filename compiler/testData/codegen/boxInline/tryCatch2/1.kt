fun test1(): Int {
    val res = My(111).performWithFail<My, Int>(
            {
                throw RuntimeException()
            }, {
                it.value
            })
    return res
}

fun test11(): Int {
    val res = My(111).performWithFail2<My, Int>(
            {
                try {
                    throw RuntimeException("1")
                } catch (e: RuntimeException) {
                    throw RuntimeException("2")
                }
            },
            {(ex, thizz) ->
                if (ex.getMessage() == "2") {
                    thizz.value
                } else {
                    -11111
                }
            })
    return res
}

fun test2(): Int {
    val res = My(111).performWithFail<My, Int>(
            {
                it.value
            },
            {
                it.value + 1
            })
    return res
}

fun test22(): Int {
    val res = My(111).performWithFail2<My, Int>(
            {
                try {
                    throw RuntimeException("1")
                } catch (e: RuntimeException) {
                    it.value
                    111
                }
            },
            {(ex, thizz) ->
                -11111
            })

    return res
}


fun test3(): Int {
    try {
        val res = My(111).performWithFail<My, Int>(
                {
                    throw RuntimeException("-1")
                }, {
                    throw RuntimeException("-2")
                })
        return res
    } catch (e: RuntimeException) {
        return e.getMessage()?.toInt2()!!
    }
}

fun test33(): Int {
    try {
        val res = My(111).performWithFail2<My, Int>(
                {
                    try {
                        throw RuntimeException("-1")
                    } catch (e: RuntimeException) {
                        throw RuntimeException("-2")
                    }
                },
                {(ex, thizz) ->
                    if (ex.getMessage() == "-2") {
                        throw RuntimeException("-3")
                    } else {
                        -11111
                    }
                })
        return res
    } catch (e: RuntimeException) {
        return e.getMessage()!!.toInt2()!!
    }
}

fun box(): String {
    if (test1() != 111) return "test1: ${test1()}"
    if (test11() != 111) return "test11: ${test11()}"

    if (test2() != 111) return "test2: ${test2()}"
    if (test22() != 111) return "test22: ${test22()}"

    if (test3() != -2) return "test3: ${test3()}"
    if (test33() != -3) return "test33: ${test33()}"

    return "OK"
}