// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = ""

    var count = 0
    fun expect(i: Int) {
        if (++count != i) throw Exception("EXPECTED $i")
    }

    fun <T> log(value: T) {
        result += "$value"
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.result += "return;"
    })
    return controller.result
}

suspend fun Controller.makeException(i: Int): Throwable? {
    expect(i)
    return Error("CHECK")
}
suspend fun Controller.rethrowException(i: Int, t: Throwable?) {
    expect(i)
    t?.let { throw it }
}

suspend fun Controller.test() {
    var exception: Throwable? = null
    expect(1)
    try {
        exception = makeException(2)
        log("try(t);")
    } finally {
        // Separate method because of KT-32220
        rethrowException(3, exception)
        log("FAIL2")
        exception?.let { throw it }
    }
    log("FAIL3")
}

fun box(): String {
    val res = builder {
        try {
            log("try;")
            test()
        } catch (e: Error) {
            log("catch;")
        }
    }

    if (res != "try;try(t);catch;return;") return "FAIL: $res"
    return "OK"
}
