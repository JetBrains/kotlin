package foo

external fun Function(vararg argsAndCode: String): Function

external interface Function {

    // JsFun("(f, a) -> f(a)")
    @nativeInvoke
    operator fun invoke(a: Int?): Int?

    @nativeInvoke
    operator fun invoke(a: String?): String?

    @nativeInvoke
    operator fun invoke(a: String?, b: Int?, c: Int? = definedExternally): String?

    @nativeInvoke
    operator fun invoke(a: Int, b: Int, c: Int): Int

    @nativeInvoke
    fun baz(a: Int?, b: Int? = definedExternally, c: Int? = definedExternally): Int?

    @nativeInvoke
    fun bar(a: String?, b: Int? = definedExternally, c: Int? = definedExternally): String?
}

fun box(): String {
    val f = Function("a", "return a")
    val g = Function("a", "b", "c", "return a + (b || 10) + (c || 100)")

    assertEquals(1, f(1))
    assertEquals("ok", f("ok"))

    assertEquals("ok34100", g("ok", 34)) // g("ok", 34, null)

    assertEquals(10, g(1, 4, 5))
    assertEquals("ok3410", g("ok", 34, 10))

    assertEquals(105, g.baz(1, 4)) // g(1, 4, null)
    assertEquals(45, g.baz(1, c = 34)) // g(1, null, 34)

    assertEquals("ok34100", g.bar("ok", 34)) // g("ok", 34, null)
    assertEquals("ok1034", g.bar("ok", 10, 34)) // g("ok", 10, 34)

    return "OK"
}
