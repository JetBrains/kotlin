import kotlin.test.*



fun box() {
    assertFalse("sample".equals("Sample", ignoreCase = false))
    assertTrue("sample".equals("Sample", ignoreCase = true))
    assertFalse("sample".equals(null, ignoreCase = false))
    assertFalse("sample".equals(null, ignoreCase = true))
    assertTrue(null.equals(null, ignoreCase = true))
    assertTrue(null.equals(null, ignoreCase = false))
}
