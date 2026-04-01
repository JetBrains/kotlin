// This test depends on line numbers.
// TARGET_BACKEND: JVM
// WITH_STDLIB
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
        if ("chainCalls.kt:12" != actual) {
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
        if ("chainCalls.kt:27" != actual) {
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
        if ("chainCalls.kt:40" != actual) {
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
        if ("chainCalls.kt:52" != actual) {
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
