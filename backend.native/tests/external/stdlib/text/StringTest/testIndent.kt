import kotlin.test.*



fun box() {
    assertEquals("  ABC\n  123", "ABC\n123".prependIndent("  "))
    assertEquals("  ABC\n  \n  123", "ABC\n\n123".prependIndent("  "))
    assertEquals("  ABC\n  \n  123", "ABC\n \n123".prependIndent("  "))
    assertEquals("  ABC\n   \n  123", "ABC\n   \n123".prependIndent("  "))
    assertEquals("  ", "".prependIndent("  "))
}
