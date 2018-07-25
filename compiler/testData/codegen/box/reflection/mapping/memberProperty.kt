// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class K(var value: Long)

fun box(): String {
    val p = K::value

    assert(p.javaField != null) { "Fail p field" }

    val getter = p.javaGetter!!
    val setter = p.javaSetter!!

    assertEquals(getter, K::class.java.getMethod("getValue"))
    assertEquals(setter, K::class.java.getMethod("setValue", Long::class.java))

    val k = K(42L)
    assert(getter.invoke(k) == 42L) { "Fail k getter" }
    setter.invoke(k, -239L)
    assert(getter.invoke(k) == -239L) { "Fail k setter" }

    return "OK"
}
