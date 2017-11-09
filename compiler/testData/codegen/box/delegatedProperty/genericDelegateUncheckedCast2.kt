// IGNORE_BACKEND: JVM, JS

import kotlin.reflect.KProperty

class Delegate<T>(var inner: T) {
    operator fun getValue(t: Any?, p: KProperty<*>): T = inner
    operator fun setValue(t: Any?, p: KProperty<*>, i: T) { inner = i }
}

val del = Delegate("zzz")

class A {
    inner class B {
        var prop: String by del
    }
}

inline fun asFailsWithCCE(block: () -> Unit) {
    try {
        block()
    }
    catch (e: ClassCastException) {
        return
    }
    catch (e: Throwable) {
        throw AssertionError("Should throw ClassCastException, got $e")
    }
    throw AssertionError("Should throw ClassCastException, no exception thrown")
}

fun box(): String {
    val c = A().B()

    (del as Delegate<String?>).inner = null
    asFailsWithCCE { c.prop }  // does not fail in JVM, JS due KT-8135.

    return "OK"
}