// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class TestException(message: String) : RuntimeException(message)

var globalCont: Continuation<String>? = null

suspend fun suspendString(): String = suspendCoroutine { cont ->
    globalCont = cont
}

fun box(): String {
    // 1. Suspension in catch block: normal completion
    var result1 = ""
    val l1: suspend () -> String = {
        try {
            throw TestException("inner")
        } catch (e: TestException) {
            suspendString() + " caught"
        }
    }
    l1.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        result1 = res.getOrThrow()
    })
    globalCont?.resume("hello")
    if (result1 != "hello caught") return "FAIL 1: $result1"

    // 2. Suspension in catch block: resumed with failure propagates out
    var thrown2: Throwable? = null
    val l2: suspend () -> String = {
        try {
            throw TestException("inner")
        } catch (e: TestException) {
            suspendString()
        }
    }
    l2.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        thrown2 = res.exceptionOrNull()
    })
    globalCont?.resumeWith(Result.failure(TestException("from resume")))
    if (thrown2 !is TestException || thrown2?.message != "from resume") {
        return "FAIL 2: $thrown2"
    }

    // 3. Suspension in catch block enclosed in outer try-catch: caught by outer catch
    var result3 = ""
    val l3: suspend () -> String = {
        try {
            try {
                throw TestException("inner")
            } catch (e: TestException) {
                suspendString()
            }
        } catch (e: TestException) {
            "outer caught: ${e.message}"
        }
    }
    l3.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        result3 = res.getOrThrow()
    })
    globalCont?.resumeWith(Result.failure(TestException("from resume 3")))
    if (result3 != "outer caught: from resume 3") return "FAIL 3: $result3"

    // 4. Suspension in finally block: normal path resumed
    var result4 = ""
    var finallyRun4 = false
    val l4: suspend () -> String = {
        try {
            "tryResult"
        } finally {
            val s = suspendString()
            if (s == "resume4") {
                finallyRun4 = true
            }
        }
    }
    l4.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        result4 = res.getOrThrow()
    })
    globalCont?.resume("resume4")
    if (result4 != "tryResult" || !finallyRun4) return "FAIL 4: $result4, $finallyRun4"

    // 5. Suspension in finally block: normal path resumed with failure propagates
    var thrown5: Throwable? = null
    val l5: suspend () -> String = {
        try {
            "tryResult"
        } finally {
            suspendString()
        }
    }
    l5.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        thrown5 = res.exceptionOrNull()
    })
    globalCont?.resumeWith(Result.failure(TestException("finally failure")))
    if (thrown5 !is TestException || thrown5?.message != "finally failure") {
        return "FAIL 5: $thrown5"
    }

    // 6. Suspension in finally block on exceptional path: try exception propagates if finally succeeds
    var thrown6: Throwable? = null
    val l6: suspend () -> String = {
        try {
            throw TestException("try exception")
        } finally {
            suspendString()
        }
    }
    l6.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        thrown6 = res.exceptionOrNull()
    })
    globalCont?.resume("ok")
    if (thrown6 !is TestException || thrown6?.message != "try exception") {
        return "FAIL 6: $thrown6"
    }

    // 7. Suspension in finally block on exceptional path: finally exception propagates if finally fails
    var thrown7: Throwable? = null
    val l7: suspend () -> String = {
        try {
            throw TestException("try exception")
        } finally {
            suspendString()
        }
    }
    l7.startCoroutine(Continuation(EmptyCoroutineContext) { res ->
        thrown7 = res.exceptionOrNull()
    })
    globalCont?.resumeWith(Result.failure(TestException("finally failure on exceptional path")))
    if (thrown7 !is TestException || thrown7?.message != "finally failure on exceptional path") {
        return "FAIL 7: $thrown7"
    }

    return "OK"
}

