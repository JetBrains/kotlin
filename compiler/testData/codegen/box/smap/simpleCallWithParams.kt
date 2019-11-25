// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
package test

fun testProperLineNumberAfterInline(): String {
    var exceptionCount = 0;
    try {
        fail(inlineFun(),
             "12")
    }
    catch(e: AssertionError) {
        val entry = (e as java.lang.Throwable).getStackTrace()!!.get(1)
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:10" != actual) {
            return "fail 1: ${actual}"
        }
        exceptionCount++
    }

    try {
        fail("12",
             inlineFun())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:23" != actual) {
            return "fail 2: ${actual}"
        }
        exceptionCount++
    }

    return if (exceptionCount == 2) "OK" else "fail"
}

fun testProperLineForOtherParameters(): String {
    var exceptionCount = 0;
    try {
        fail(inlineFun(),
             fail())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:42" != actual) {
            return "fail 3: ${actual}"
        }
        exceptionCount++

    }

    try {
        fail(fail(),
             inlineFun())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:55" != actual) {
            return "fail 4: ${actual}"
        }
        exceptionCount++
    }

    try {
        fail(fail(), inlineFun())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:68" != actual) {
            return "fail 5: ${actual}"
        }
        exceptionCount++
    }

    try {
        fail(fail(), inlineFun())
    }
    catch(e: AssertionError) {
        val entry = e.stackTrace!![1]
        val actual = "${entry.getFileName()}:${entry.getLineNumber()}"
        if ("simpleCallWithParams.kt:80" != actual) {
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

public fun fail(p1: String, p2: String) {
    throw AssertionError("fail")
}

inline fun inlineFun(): String {
    return "123"
}

fun fail(): String {
    throw AssertionError("fail")
}
