// !LANGUAGE: +SuspendConversion
// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KCallable

private fun check(label: String, fn: Any) {
    if (fn is KCallable<*>) {
        throw AssertionError("$label is KCallable, ${fn::class.java.simpleName}")
    }
}

fun checkSuspend(label: String, fn: suspend () -> Unit) = check(label, fn)
fun checkSuspendAny(label: String, fn: suspend () -> Any) = check(label, fn)

fun withSuspendConversion() {}

class CCtorAsSuspend()

fun box(): String {
    checkSuspend("::withSuspendConversion", ::withSuspendConversion)
    checkSuspendAny("::CCtorAsSuspend", ::CCtorAsSuspend)

    return "OK"
}
