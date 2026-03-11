// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KCallable

private fun check(label: String, fn: Any) {
    if (fn !is KCallable<*>) {
        throw AssertionError("$label is not KCallable, ${fn::class.java.simpleName}")
    }
}

fun checkVarargAsArray(label: String, fn: (IntArray) -> C) = check(label, fn)

fun withVarargs(vararg xs: Int): C = C(*xs)
class C(vararg xs: Int)

fun box(): String {
    checkVarargAsArray("::withVarargs", ::withVarargs)
    checkVarargAsArray("::C", ::C)

    return "OK"
}
