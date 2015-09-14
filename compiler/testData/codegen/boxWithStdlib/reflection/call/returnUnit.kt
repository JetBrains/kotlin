import kotlin.test.assertEquals

fun foo() {}

class A {
    fun bar() {}
}

object O {
    @kotlin.platform.platformStatic fun baz() {}
}

fun nullableUnit(unit: Boolean): Unit? = if (unit) Unit else null

fun box(): String {
    assertEquals(Unit, ::foo.call())
    assertEquals(Unit, A::bar.call(A()))
    assertEquals(Unit, O::baz.call(O))

    assertEquals(Unit, (::nullableUnit).call(true))
    assertEquals(null, (::nullableUnit).call(false))

    return "OK"
}
