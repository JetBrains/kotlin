// WITH_STDLIB
// WITH_REFLECT

import kotlin.test.*
import kotlin.reflect.KClass

enum class E(val arg: KClass<*>?) {
    A(null as KClass<*>?),
    B(String::class);
}

fun box(): String {
    assertEquals("String", E.B.arg?.simpleName)

    return "OK"
}
