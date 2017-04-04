import kotlin.test.*



fun box() {
    val data = "abcd1234"
    assertEquals("abc", data.take(3))
    assertFails {
        data.take(-7)
    }
    assertEquals(data, data.take(data.length + 42))
}
