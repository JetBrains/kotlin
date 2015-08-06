import kotlin.test.assertEquals

fun box(): String {
    val f = J::x
    assertEquals("x", f.name)

    assertEquals(f, J::class.members.single { it.name == "x" })

    f.set("OK")
    assertEquals("OK", J.x)
    assertEquals("OK", f.getter())

    return f.get()
}
