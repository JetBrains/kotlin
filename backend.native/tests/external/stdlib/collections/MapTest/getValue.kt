import kotlin.test.*


fun box() {
    val data: MutableMap<String, Int> = hashMapOf("bar" to 1)
    assertFailsWith<NoSuchElementException> { data.getValue("foo") }.let { e ->
        assertTrue("foo" in e.message!!)
    }
    assertEquals(1, data.getValue("bar"))

    val mutableWithDefault = data.withDefault { 42 }
    assertEquals(42, mutableWithDefault.getValue("foo"))

    // verify that it is wrapper
    mutableWithDefault["bar"] = 2
    assertEquals(2, data["bar"])
    data["bar"] = 3
    assertEquals(3, mutableWithDefault["bar"])

    val readonlyWithDefault = (data as Map<String, Int>).withDefault { it.length }
    assertEquals(4, readonlyWithDefault.getValue("loop"))

    val withReplacedDefault = readonlyWithDefault.withDefault { 42 }
    assertEquals(42, withReplacedDefault.getValue("loop"))
}
