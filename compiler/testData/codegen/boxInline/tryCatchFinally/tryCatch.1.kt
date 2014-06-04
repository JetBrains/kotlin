fun test1() : Int {
    val inlineX = My(111)
    var result = 0
    val res = inlineX.perform<My, Int>{

        try {
            throw RuntimeException()
        } catch (e: RuntimeException) {
            result = -1
        }
        result
    }

    return result
}

fun test11() : Int {
    val inlineX = My(111)
    val res = inlineX.perform<My, Int>{
        try {
            throw RuntimeException()
        } catch (e: RuntimeException) {
            -1
        }
    }

    return res
}

fun test2() : Int {
    try {
        val inlineX = My(111)
        var result = 0
        val res = inlineX.perform<My, Int>{
            try {
                throw RuntimeException("-1")
            } catch (e: RuntimeException) {
                throw RuntimeException("-2")
            }
        }
        return result
    } catch (e: RuntimeException) {
        return e.getMessage()!!.toInt2()!!
    }
}

fun box(): String {
    if (test1() != -1) return "test1: ${test1()}"
    if (test11() != -1) return "test11: ${test11()}"
    if (test2() != -2) return "test2: ${test2()}"

    return "OK"
}