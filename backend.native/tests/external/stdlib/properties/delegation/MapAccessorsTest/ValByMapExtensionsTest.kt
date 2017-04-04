import kotlin.test.*

class ValByMapExtensionsTest {
    val map: Map<String, String> = hashMapOf("a" to "all", "b" to "bar", "c" to "code")
    val genericMap = mapOf<String, Any?>("i" to 1, "x" to 1.0)

    val a by map
    val b: String by map
    val c: Any by map
    val d: String? by map
    val e: String by map.withDefault { "default" }
    val f: String? by map.withDefault { null }
    // val n: Int by map // prohibited by type system
    val i: Int by genericMap
    val x: Double by genericMap


    fun doTest() {
        assertEquals("all", a)
        assertEquals("bar", b)
        assertEquals("code", c)
        assertEquals("default", e)
        assertEquals(null, f)
        assertEquals(1, i)
        assertEquals(1.0, x)
        assertFailsWith<NoSuchElementException> { d }
    }
}

fun box() {
    ValByMapExtensionsTest().doTest()
}