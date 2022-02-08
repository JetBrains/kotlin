// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.reflect.KCallable

private fun check(label: String, fn: Any) {
    if (fn is KCallable<*>) {
        throw AssertionError("$label is KCallable, ${fn::class.java.simpleName}")
    }
}

fun checkUnit(label: String, fn: () -> Unit) = check(label, fn)
fun checkAny(label: String, fn: () -> Any) = check(label, fn)
fun checkOneElementForVararg(label: String, fn: (Int) -> Unit) = check(label, fn)

fun withDefaults(a: Int = 1, b: Int = 2) {}
fun withVarargs(vararg xs: Int) {}
fun withCoercion() = 1

class CWithDefaults(x: Int = 1, y: Int = 2)
class CWithVarargs(vararg xs: Int)

fun box(): String {
    checkUnit("::withDefaults", ::withDefaults)
    checkUnit("::withVarargs", ::withVarargs)
    checkUnit("::withCoercion", ::withCoercion)

    checkAny("::CWithDefaults", ::CWithDefaults)
    checkAny("::CWithVarargs", ::CWithVarargs)

    checkUnit("::CWithDefaults", ::CWithDefaults)
    checkUnit("::CWithVarargs", ::CWithVarargs)

    checkOneElementForVararg("::withVarargs", ::withVarargs)
    checkOneElementForVararg("::CWithVarargs", ::CWithVarargs)

    return "OK"
}
