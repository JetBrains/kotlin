// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// FULL_JDK
package test
fun testProperLineNumberAfterInline(): String {
    var exceptionCount = 0;
    try {
        checkEquals(test(),
                    "12")
    }
    catch(e: AssertionError) {
        val entry = (e as java.lang.Throwable).getStackTrace()!!.get(1)
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:7" != actual) {
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
        if ("simpleCallWithParams.kt:20" != actual) {
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
        if ("simpleCallWithParams.kt:38" != actual) {
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
        if ("simpleCallWithParams.kt:52" != actual) {
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
        if ("simpleCallWithParams.kt:65" != actual) {
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
        if ("simpleCallWithParams.kt:77" != actual) {
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
