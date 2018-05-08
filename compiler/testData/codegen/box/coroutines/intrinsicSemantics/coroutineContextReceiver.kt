// !API_VERSION: 1.2
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
import kotlin.test.assertEquals

@Suppress("DEPRECATION_ERROR")
class Controller {
    suspend fun controllerSuspendHereOld() =
        if (kotlin.coroutines.experimental.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerMultipleArgsOld(a: Any, b: Any, c: Any) =
        if (kotlin.coroutines.experimental.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerSuspendHereNew() =
        if (kotlin.coroutines.experimental.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerMultipleArgsNew(a: Any, b: Any, c: Any) =
        if (kotlin.coroutines.experimental.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    fun builder(c: suspend Controller.() -> String): String {
        var fromSuspension: String? = null

        c.startCoroutine(this, object : Continuation<String> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWithException(exception: Throwable) {
                fromSuspension = "Exception: " + exception.message!!
            }

            override fun resume(value: String) {
                fromSuspension = value
            }
        })

        return fromSuspension as String
    }
}

fun box(): String {
    val c = Controller()
    var res = c.builder { controllerSuspendHereOld() }
    if (res != "OK") {
        return "fail 1 $res"
    }
    res = c.builder { controllerMultipleArgsOld(1, 1, 1) }
    if (res != "OK") {
        return "fail 2 $res"
    }
    res = c.builder {
        @Suppress("DEPRECATION_ERROR")
        if (kotlin.coroutines.experimental.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"
    }
    if (res != "OK") {
        return "fail 3 $res"
    }
    res = c.builder { controllerSuspendHereNew() }
    if (res != "OK") {
        return "fail 4 $res"
    }
    res = c.builder { controllerMultipleArgsNew(1, 1, 1) }
    if (res != "OK") {
        return "fail 5 $res"
    }
    res = c.builder {
        if (kotlin.coroutines.experimental.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"
    }
    if (res != "OK") {
        return "fail 6 $res"
    }

    return "OK"
}