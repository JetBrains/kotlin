// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

suspend fun callLocal(): String {
    suspend fun local(): String {
        suspend fun local(): String {
            suspend fun local(): String {
                suspend fun local(): String {
                    suspend fun local(): String {
                        return "OK"
                    }
                    return local()
                }
                return local()
            }
            return local()
        }
        return local()
    }
    return local()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = callLocal()
    }
    return res
}
