// WITH_RUNTIME
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
class Controller {
    suspend fun suspendHere() = suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
    }

    suspend fun tailCall(): String {
        return suspendHere()
    }

    suspend fun nonTailCall(): String {
        suspendHere()

        return "OK"
    }

    suspend fun multipleSuspensions(): String {
        suspendHere()
        return suspendHere()
    }
}

fun builder(c: suspend Controller.() -> Unit) {

}

fun box(): String {
    var result = ""

    builder { ->
        val z = ""
        val u = 1L
        result = suspendHere()

        result += z + u
    }

    return result
}
