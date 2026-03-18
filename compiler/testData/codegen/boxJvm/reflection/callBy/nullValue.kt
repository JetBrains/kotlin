// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertNull

fun foo(x: String? = "Fail") {
    assertNull(x)
}

fun box(): String {
    ::foo.callBy(mapOf(::foo.parameters.single() to null))
    return "OK"
}
