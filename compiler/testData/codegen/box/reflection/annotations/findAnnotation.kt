// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.test.assertEquals
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

    assertEquals("OK", Foo::class.findAnnotations<Yes>().single().value)

    return Foo::class.findAnnotation<Yes>()?.value ?: "Fail: no annotation"
}
