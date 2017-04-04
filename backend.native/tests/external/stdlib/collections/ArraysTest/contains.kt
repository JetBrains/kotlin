import kotlin.test.*

fun box() {
    assertTrue(arrayOf("1", "2", "3", "4").contains("2"))
    assertTrue("3" in arrayOf("1", "2", "3", "4"))
    assertTrue("0" !in arrayOf("1", "2", "3", "4"))
}
