// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.full.hasAnnotation
import kotlin.test.assertFalse
import kotlin.test.assertTrue

annotation class Baz
annotation class Far

@Baz
@Far
class Foo

class Bar

fun box(): String {
    assertFalse(Bar::class.hasAnnotation<Baz>())
    assertFalse(Bar::class.hasAnnotation<Far>())

    assertTrue(Foo::class.hasAnnotation<Baz>())
    assertTrue(Foo::class.hasAnnotation<Far>())

    return "OK"
}
