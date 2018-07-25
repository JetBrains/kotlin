// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.test.assertEquals

object Obj {
    @JvmStatic
    fun foo(a: String, b: String = "b") = a + b
}

fun box(): String {
    val f = Obj::class.members.single { it.name == "foo" }

    // Any object method currently requires the object instance passed
    try {
        f.callBy(mapOf(
                f.parameters.single { it.name == "a" } to "a"
        ))
        return "Fail: IllegalArgumentException should have been thrown"
    }
    catch (e: IllegalArgumentException) {
        // OK
    }

    assertEquals("ab", f.callBy(mapOf(
            f.parameters.first() to Obj,
            f.parameters.single { it.name == "a" } to "a"
    )))

    return "OK"
}
