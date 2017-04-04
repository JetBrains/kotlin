import kotlin.test.*


fun box() {
    assertEquals(listOf("some", "cat", "do"), arrayOf("some", "case", "cat", "do", "dog", "it").distinctBy { it.length })
    assertTrue(charArrayOf().distinctBy { it }.isEmpty())
}
