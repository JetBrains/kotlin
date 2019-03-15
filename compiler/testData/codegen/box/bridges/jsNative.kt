// TARGET_BACKEND: JS
package foo

external interface A {
    fun foo(value: Int): String
}

interface B {
    fun foo(value: Int): String
}

class C : A, B {
    override fun foo(value: Int) = "C.foo($value)"
}

open class D {
    open fun foo(value: Int) = "D.foo($value)"
}

class E : D(), A, B

fun box(): String {
    val a: A = C()
    assertEquals("C.foo(55)", a.foo(55))

    val b: B = C()
    assertEquals("C.foo(23)", b.foo(23))

    val d: dynamic = C()
    assertEquals("C.foo(42)", d.foo(42))
    if (testUtils.isLegacyBackend()) {
        assertEquals("C.foo(99)", d.`foo_za3lpa$`(99))
    }

    val da: A = E()
    assertEquals("D.foo(55)", da.foo(55))

    val db: B = E()
    assertEquals("D.foo(23)", db.foo(23))

    val dd: dynamic = E()
    assertEquals("D.foo(42)", dd.foo(42))

    if (testUtils.isLegacyBackend()) {
        assertEquals("D.foo(99)", dd.`foo_za3lpa$`(99))
    }

    return "OK"
}