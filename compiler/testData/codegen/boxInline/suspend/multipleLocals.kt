// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: inlined.kt
object Result {
    var a: String = ""
    var b: Int = 0
}

// Needed for JS compatibility
interface Runnable {
    fun run(): Unit
}

suspend inline fun inlineMe(c: suspend () -> Unit) {
    var a = ""
    var b = 0
    val r = object: Runnable {
        override fun run() {
            b++
            a += "a"
        }
    }
    r.run()
    c()
    r.run()
    Result.a = a
    Result.b = b
}
suspend inline fun noinlineMe(noinline c: suspend () -> Unit) {
    var a = ""
    var b = 0
    val r = object: Runnable {
        override fun run() {
            b += 2
            a += "b"
        }
    }
    r.run()
    c()
    r.run()
    Result.a += a
    Result.b += b
}
suspend inline fun crossinlineMe(crossinline c: suspend () -> Unit) {
    var a = ""
    var b = 0
    val r = object: Runnable {
        override fun run() {
            b += 3
            a += "c"
        }
    }
    r.run()
    c()
    r.run()
    Result.a += a
    Result.b += b
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun dummy() {
    val local0 = 0
    val local1 = 0
    val local2 = 0
    val local3 = 0
    val local4 = 0
    val local5 = 0
    val local6 = 0
    val local7 = 0
    val local8 = 0
    val local9 = 0
    val local10 = 0
    val local11 = 0
    val local12 = 0
    val local13 = 0
    val local14 = 0
    val local15 = 0
    val local16 = 0
    val local17 = 0
    val local18 = 0
    val local19 = 0
    val local20 = 0
    val local21 = 0
    val local22 = 0
}

suspend fun inlineSite() {
    inlineMe {
        dummy()
        dummy()
    }
    if (Result.a != "aa" || Result.b != 2) throw RuntimeException("FAIL 1")
    noinlineMe {
        dummy()
        dummy()
    }
    if (Result.a != "aabb" || Result.b != 6) throw RuntimeException("FAIL 2")
    crossinlineMe {
        dummy()
        dummy()
    }
    if (Result.a != "aabbcc" || Result.b != 12) throw RuntimeException("FAIL 3")
}

fun box(): String {
    builder {
        inlineSite()
    }
    return "OK"
}
