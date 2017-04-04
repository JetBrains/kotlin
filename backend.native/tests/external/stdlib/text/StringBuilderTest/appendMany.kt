import kotlin.test.*


fun box() {
    assertEquals("a1", StringBuilder().append("a", "1").toString())
    assertEquals("a1", StringBuilder().append("a", 1).toString())
    assertEquals("a1", StringBuilder().append("a", StringBuilder().append("1")).toString())
}
