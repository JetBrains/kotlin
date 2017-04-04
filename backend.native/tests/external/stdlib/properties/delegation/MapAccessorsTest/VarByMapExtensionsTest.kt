import kotlin.test.*

class VarByMapExtensionsTest {
    val map = hashMapOf<String, Any?>("a" to "all", "b" to null, "c" to 1, "xProperty" to 1.0)
    val map2: MutableMap<String, CharSequence> = hashMapOf("a2" to "all")

    var a: String by map
    var b: Any? by map
    var c: Int by map
    var d: String? by map
    var a2: String by map2.withDefault { "empty" }
    //var x: Int by map2  // prohibited by type system

    fun doTest() {
        assertEquals("all", a)
        assertEquals(null, b)
        assertEquals(1, c)
        c = 2
        assertEquals(2, c)
        assertEquals(2, map["c"])

        assertEquals("all", a2)
        map2.remove("a2")
        assertEquals("empty", a2)

        map["c"] = "string"
        // fails { c }  // does not fail in JS due to KT-8135

        map["a"] = null
        a // fails { a } // does not fail due to KT-8135

        assertFailsWith<NoSuchElementException> { d }
        map["d"] = null
        assertEquals(null, d)
    }
}

fun box() {
    VarByMapExtensionsTest().doTest()
}
