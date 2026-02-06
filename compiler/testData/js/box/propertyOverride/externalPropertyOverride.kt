external interface A {
    @JsName("__name")
    val name: String

    @JsName("bar")
    fun foo(): String
}

external class B: A {
    override val name: String
    override fun foo(): String
}

fun box(): String {
    val c = js("{ __name: 'Frodo', bar: function() { return 'Baggins' } }")

    val a: A = c
    val b: B = c

    assertEquals(a.name, "Frodo")
    assertEquals(a.asDynamic().__name, "Frodo")

    assertEquals(b.name, "Frodo")
    assertEquals(b.asDynamic().__name, "Frodo")

    assertEquals(a.foo(), "Baggins")
    assertEquals(a.asDynamic().bar(), "Baggins")

    assertEquals(b.foo(), "Baggins")
    assertEquals(b.asDynamic().bar(), "Baggins")

    return "OK"
}