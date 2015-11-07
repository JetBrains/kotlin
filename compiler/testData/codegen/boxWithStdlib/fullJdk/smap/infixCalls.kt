fun testProperLineNumber(): String {
    var exceptionCount = 0;
    try {
        test() fail
                call()
    }
    catch(e: AssertionError) {
        val entry = (e as java.lang.Throwable).getStackTrace()!!.get(1)
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("infixCalls.kt:4" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    try {
        call() fail
                test()
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("infixCalls.kt:17" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    try {
        call() fail test()
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("infixCalls.kt:30" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    return if (exceptionCount == 3) "OK" else "fail"
}

fun box(): String {
    return testProperLineNumber()
}

public fun checkEquals(p1: String, p2: String) {
    throw AssertionError("fail")
}

inline fun test(): String {
    return "123"
}

fun String.fail(p: String): String {
    throw AssertionError("fail")
}

fun call(): String {
    return "xxx"
}