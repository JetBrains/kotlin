// !API_VERSION: 1.2
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
import kotlin.test.assertEquals

@Suppress("DEPRECATION_ERROR")
class Controller {
    val coroutineContext = object : CoroutineContext {
        public override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? = null
        public override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R = initial
        public override fun plus(context: CoroutineContext): CoroutineContext = context
        public override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext = this
        public override fun hashCode(): Int = 0
        public override fun toString(): String = "AnotherEmptyCoroutineContext"
    }

    suspend fun controllerSuspendHere() =
        if (coroutineContext == EmptyCoroutineContext) "$coroutineContext == $EmptyCoroutineContext" else "OK"

    suspend fun controllerSuspendHereIntrinsicOld() =
        if (kotlin.coroutines.experimental.intrinsics.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.intrinsics.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerSuspendHereIntrinsicNew() =
        if (kotlin.coroutines.experimental.coroutineContext != EmptyCoroutineContext)
            "${kotlin.coroutines.experimental.coroutineContext} != $EmptyCoroutineContext"
        else
            "OK"

    suspend fun controllerMultipleArgs(a: Any, b: Any, c: Any) =
        if (coroutineContext == EmptyCoroutineContext) "$coroutineContext == $EmptyCoroutineContext" else "OK"

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
    val v = Controller()
    var res = v.builder { controllerMultipleArgs(1, 1, 1) }
    if (res != "OK") {
        return "fail 1 $res"
    }
    res = v.builder { if (coroutineContext == EmptyCoroutineContext) "$coroutineContext == $EmptyCoroutineContext" else "OK" }
    if (res != "OK") {
        return "fail 2 $res"
    }
    res = v.builder { controllerSuspendHereIntrinsicOld() }
    if (res != "OK") {
        return "fail 3 $res"
    }
    res = v.builder { controllerSuspendHereIntrinsicNew() }
    if (res != "OK") {
        return "fail 4 $res"
    }
    res = v.builder { controllerSuspendHere() }
    if (res != "OK") {
        return "fail 5 $res"
    }

    return "OK"
}