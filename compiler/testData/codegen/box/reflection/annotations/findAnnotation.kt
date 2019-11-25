// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import kotlin.reflect.full.findAnnotation
import kotlin.test.assertNull

annotation class Yes(val value: String)
annotation class No(val value: String)

@Yes("OK")
@No("Fail")
class Foo

class Bar

fun box(): String {
    assertNull(Bar::class.findAnnotation<Yes>())
    assertNull(Bar::class.findAnnotation<No>())

    return Foo::class.findAnnotation<Yes>()?.value ?: "Fail: no annotation"
}
