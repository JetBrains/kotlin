// FULL_JDK

fun testProperLineNumberAfterInline(): String {
    var exceptionCount = 0;
    try {
        checkEquals(test(),
                    "12")
    }
    catch(e: AssertionError) {
        val entry = (e as java.lang.Throwable).getStackTrace()!!.get(1)
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:6" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    try {
        checkEquals("12",
                    test())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:19" != actual) {
            return "fail 2: ${actual}"
        }
        exceptionCount++
    }

    return if (exceptionCount == 2) "OK" else "fail"
}

fun testProperLineForOtherParameters(): String {
    var exceptionCount = 0;
    try {
        checkEquals(test(),
                    fail())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:37" != actual) {
            return "fail 3: ${actual}"
        }
        exceptionCount++

    }

    try {
        checkEquals(fail(),
                    test())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:51" != actual) {
            return "fail 4: ${actual}"
        }
        exceptionCount++
    }

    try {
        checkEquals(fail(), test())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:64" != actual) {
            return "fail 5: ${actual}"
        }
        exceptionCount++
    }

    try {
        checkEquals(fail(), test())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:76" != actual) {
            return "fail 6: ${actual}"
        }
        exceptionCount++
    }

    return if (exceptionCount == 4) "OK" else "fail"
}


fun box(): String {
    val res = testProperLineNumberAfterInline()
    if (res != "OK") return "$res"

    return testProperLineForOtherParameters()
}

public fun checkEquals(p1: String, p2: String) {
    throw AssertionError("fail")
}

inline fun test(): String {
    return "123"
}

fun fail(): String {
    throw AssertionError("fail")
}
