// WITH_STDLIB

import kotlin.test.*

class ValByMapExtensionsTest {
    val map: Map<String, String> = hashMapOf("a" to "all", "b" to "bar", "c" to "code")
    val genericMap = mapOf<String, Any?>("i" to 1, "x" to 1.0)
    val mmapOut: MutableMap<String, out String> = mutableMapOf("g" to "out", "g1" to "in")
    val genericMmapOut: MutableMap<String, out Any?> = mmapOut

    val a by map
    val b: String by map
    val c: Any by map
    val d: String? by map
    val e: String by map.withDefault { "default" }
    val f: String? by map.withDefault { null }
    val g: String by mmapOut
    val g1: String by genericMmapOut

    val i: Int by genericMap
    val x: Double by genericMap

    fun doTest() {
        assertEquals("all", a)
        assertEquals("bar", b)
        assertEquals("code", c)
        assertEquals("default", e)
        assertEquals(null, f)
        assertEquals("out", g)
        assertEquals("in", g1)
        assertEquals(1, i)
        assertEquals(1.0, x)
        assertFailsWith<NoSuchElementException> { d }
    }
}

fun box(): String {
    ValByMapExtensionsTest().doTest()
    return "OK"
}
