import kotlin.test.*


fun box() {
    val range = "island".."isle"
    assertFalse("apple" in range)
    assertFalse("icicle" in range)

    assertTrue("island" in range)
    assertTrue("isle" in range)
    assertTrue("islandic" in range)

    assertFalse("item" in range)
    assertFalse("trail" in range)

    assertFalse(range.isEmpty())
}
