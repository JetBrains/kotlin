import kotlin.test.assertEquals

fun box(): String {
    val f = J::x
    assertEquals("x", f.name)

    f.set("OK")
    assertEquals("OK", J.x)
    assertEquals("OK", f.getter())

    return f.get()
}
