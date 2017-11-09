// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

import kotlin.reflect.KClass
import kotlin.test.*

class M

fun check(x: KClass<*>) {
    assertEquals(x, x.java.kotlin)
    assertEquals(x.hashCode(), x.java.kotlin.hashCode())
    assertEquals(x.java.toString() + " (Kotlin reflection is not available)", x.toString())
}

fun box(): String {
    check(M::class)
    check(String::class)
    check(Error::class)
    check(Int::class)
    check(java.lang.Integer::class)
    check(MutableList::class)
    check(Array<String>::class)

    return "OK"
}
