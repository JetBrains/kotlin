// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible

class A {
    private var value = 0

    fun ref() = A::class.memberProperties.single() as KMutableProperty1<A, Int>
}

fun box(): String {
    val a = A()
    val p = a.ref()
    try {
        p.set(a, 1)
        return "Fail: private property is accessible by default"
    } catch(e: IllegalCallableAccessException) { }

    p.isAccessible = true

    p.set(a, 2)
    p.get(a)

    p.isAccessible = false
    try {
        p.set(a, 3)
        return "Fail: setAccessible(false) had no effect"
    } catch(e: IllegalCallableAccessException) { }

    return "OK"
}
