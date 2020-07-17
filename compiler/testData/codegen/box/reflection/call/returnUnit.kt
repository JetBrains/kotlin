// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.assertEquals

fun foo() {}

class A {
    fun bar() {}
}

object O {
    @JvmStatic fun baz() {}
}

fun nullableUnit(unit: Boolean): Unit? = if (unit) Unit else null

fun box(): String {
    assertEquals(Unit, ::foo.call())
    assertEquals(Unit, A::bar.call(A()))
    assertEquals(Unit, O::class.members.single { it.name == "baz" }.call(O))

    assertEquals(Unit, (::nullableUnit).call(true))
    assertEquals(null, (::nullableUnit).call(false))

    return "OK"
}
