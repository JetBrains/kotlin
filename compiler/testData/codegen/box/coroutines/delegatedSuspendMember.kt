// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = "fail"

interface SuspendInterface {
    suspend fun foo(v: String)
}

class Delegate : SuspendInterface {
    override suspend fun foo(v: String) {
        result = v
    }
}

class Decorator(parent: SuspendInterface) : SuspendInterface by parent

fun execute(c: suspend () -> Unit) = c.startCoroutine(EmptyContinuation)

fun box(): String {

    execute {
        Decorator(Delegate()).foo("OK")
    }

    return result
}