// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertNull

fun foo(x: String? = "Fail") {
    assertNull(x)
}

fun box(): String {
    ::foo.callBy(mapOf(::foo.parameters.single() to null))
    return "OK"
}
