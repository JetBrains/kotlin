// JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// JVM_TARGET: 1.8
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL
// WITH_COROUTINES
// WITH_STDLIB
import helpers.*
import kotlin.coroutines.*

@JvmDefaultWithCompatibility
interface Test {

    suspend fun suspendFun() = privateSuspendFun()

    private suspend fun privateSuspendFun() = "OK"

}

class TestClass : Test

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "FAIL"
    builder {
        val testClass = TestClass()
        result = testClass.suspendFun()
    }
    return result
}
