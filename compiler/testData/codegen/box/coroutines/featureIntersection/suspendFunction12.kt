// IGNORE_BACKEND: JS_IR, JS
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// LANGUAGE_VERSION: 1.2

import helpers.*
import kotlin.coroutines.*

val lambda1 = { x: Any -> } as (Any) -> Unit
val suspendLambda0: suspend () -> Unit = {}

fun box(): String {
    assert(lambda1 is SuspendFunction0<*>) { "Failed: lambda1 !is SuspendFunction0<*>" }
    assert(suspendLambda0 is Function1<*, *>) { "Failed: suspendLambda0 is Function1<*, *>" }
    assert(suspendLambda0 is SuspendFunction0<*>) { "Failed: suspendLambda0 is SuspendFunction0<*>" }

    return "OK"
}