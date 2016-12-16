// WITH_RUNTIME
import kotlin.coroutines.*
class Controller {
    suspend fun suspendHere() = CoroutineIntrinsics.suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
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
