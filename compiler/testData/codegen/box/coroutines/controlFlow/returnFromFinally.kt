// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*



class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        COROUTINE_SUSPENDED
    }

    var count = 0
    fun expect(i: Int) {
        if (++count != i) throw Exception("EXPECTED $i")
    }

    fun <T> log(value: T) {
        result += "log($value);"
    }
}

fun builder(c: suspend Controller.() -> Int): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.result += "return($it);"
    })
    return controller.result
}

fun box(): String {
    val res = builder {
        expect(1)
        log(1)
        try {
            expect(2)
            suspendAndLog(2)
        } finally {
            expect(3)
            log(3)
            return@builder 4
        }
        log("FAIL")
        -1
    }

    if (res != "log(1);suspend(2);log(3);return(4);") return "FAIL: $res"
    return "OK"
}