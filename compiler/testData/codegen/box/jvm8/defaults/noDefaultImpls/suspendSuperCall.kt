// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_STDLIB
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    it.resume(Unit)
    COROUTINE_SUSPENDED
}

interface MyInterface {
    suspend fun myMethod(myParam: String): String {
        suspendHere()
        return myParam
    }
}

class MyImplementation : MyInterface {
    override suspend fun myMethod(myParam: String): String {
        suspendHere()
        return super.myMethod(myParam)
    }
}

fun box(): String {
    var result = "fail"
    suspend { result = MyImplementation().myMethod("OK") }.startCoroutine(EmptyContinuation)
    return result
}
