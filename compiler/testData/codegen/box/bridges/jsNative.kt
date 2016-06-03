// TARGET_BACKEND: JS
package foo

@native interface A {
    fun foo(value: Int): String
}

interface B {
    fun foo(value: Int): String
}

class C : A, B {
    override fun foo(value: Int) = "C.foo($value)"
}

fun box(): String {
    val a: A = C()
    assertEquals("C.foo(55)", a.foo(55))

    val b: B = C()
    assertEquals("C.foo(23)", b.foo(23))

    val d: dynamic = C()
    assertEquals("C.foo(42)", d.foo(42))
    assertEquals("C.foo(99)", d.`foo_za3lpa$`(99))

    return "OK"
}