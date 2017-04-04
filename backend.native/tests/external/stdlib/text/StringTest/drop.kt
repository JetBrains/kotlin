import kotlin.test.*



fun box() {
    val data = "abcd1234"
    assertEquals("d1234", data.drop(3))
    assertFails {
        data.drop(-2)
    }
    assertEquals("", data.drop(data.length + 5))
}
