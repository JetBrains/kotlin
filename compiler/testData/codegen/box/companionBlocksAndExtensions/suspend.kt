// WITH_COROUTINES
// WITH_REFLECT
// TARGET_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.full.callSuspendBy

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    companion {
        suspend fun compBlockFun(k: String = "") = "compBlockFun: $k "
    }
}
suspend companion fun A.compExtFun(k: String = "") = "compExtFun: $k "


fun box(): String { //todo: assertEquals instead of if &&
    var res: String? = ""
    builder {
        res =  A.compBlockFun("a") + A.compBlockFun() + A.compExtFun("c") + A.compExtFun()
    }
}
