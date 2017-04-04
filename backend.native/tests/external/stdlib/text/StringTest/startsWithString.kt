import kotlin.test.*



fun box() {
    assertTrue("abcd".startsWith("ab"))
    assertTrue("abcd".startsWith("abcd"))
    assertTrue("abcd".startsWith("a"))
    assertFalse("abcd".startsWith("abcde"))
    assertFalse("abcd".startsWith("b"))
    assertFalse("".startsWith("a"))
    assertTrue("some".startsWith(""))
    assertTrue("".startsWith(""))

    assertFalse("abcd".startsWith("aB", ignoreCase = false))
    assertTrue("abcd".startsWith("aB", ignoreCase = true))
}
