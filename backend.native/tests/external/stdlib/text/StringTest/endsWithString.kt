import kotlin.test.*



fun box() {
    assertTrue("abcd".endsWith("d"))
    assertTrue("abcd".endsWith("abcd"))
    assertFalse("abcd".endsWith("b"))
    assertFalse("strö".endsWith("RÖ", ignoreCase = false))
    assertTrue("strö".endsWith("RÖ", ignoreCase = true))
    assertFalse("".endsWith("a"))
    assertTrue("some".endsWith(""))
    assertTrue("".endsWith(""))
}
