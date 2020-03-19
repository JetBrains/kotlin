// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
import kotlin.reflect.KCallable

fun checkUnit(label: String, fn: () -> Unit) {
    if (fn is KCallable<*>) {
        throw AssertionError("$label is KCallable, ${fn::class.java.simpleName}")
    }
}

fun checkAny(label: String, fn: () -> Any) {
    if (fn is KCallable<*>) {
        throw AssertionError("$label is KCallable, ${fn::class.java.simpleName}")
    }
}

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

    // TODO KT-37604
    // checkUnit("::CWithDefaults", ::CWithDefaults)
    // checkUnit("::CWithVarargs", ::CWithVarargs)

    return "OK"
}