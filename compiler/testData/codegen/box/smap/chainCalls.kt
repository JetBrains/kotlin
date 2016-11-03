// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// FULL_JDK
package test
fun testProperLineNumber(): String {
    var exceptionCount = 0;
    try {
        test().
                test().
                fail()

    }
    catch(e: AssertionError) {
        val entry = (e as java.lang.Throwable).getStackTrace()!!.get(1)
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("chainCalls.kt:9" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    try {
        call().
                test().
                fail()
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("chainCalls.kt:24" != actual) {
            return "fail 2: ${actual}"
        }
        exceptionCount++
    }

    try {
        test().
                fail()
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("chainCalls.kt:37" != actual) {
            return "fail 3: ${actual}"
        }
        exceptionCount++
    }

    try {
        test().fail()
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("chainCalls.kt:49" != actual) {
            return "fail 4: ${actual}"
        }
        exceptionCount++
    }

    return if (exceptionCount == 4) "OK" else "fail"
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

inline fun String.test(): String {
    return "123"
}

fun String.fail(): String {
    throw AssertionError("fail")
}

fun call(): String {
    return "xxx"
}
