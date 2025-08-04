// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// LANGUAGE: +ForbidBridgesConflictingWithInheritedMethodsInJvmCode
// ISSUE: KT-13712

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

open class Service {
    open suspend fun foo(p: Unit): Int = 42
}

interface Api<A> {
    suspend fun foo(p: A): Int
}

class Impl: Service(), Api<Unit>

open class ApiReversed<A> {
    open suspend fun foo (p: A): Int = 42
}

interface ServiceReversed {
    suspend fun foo(p: Unit): Int
}

class ImplReversed: ApiReversed<Unit>(), ServiceReversed

// no ClassCastException or IncompatibleClassChangeError
fun box(): String {
    var res = ""
    builder {
        res = when {
            Impl().foo(Unit) != 42 -> "Fail"
            ImplReversed().foo(Unit) != 42 -> "Fail reversed"
            else -> "OK"
        }
    }
    return res
}
