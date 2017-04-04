import kotlin.test.*

fun box() {
    val arr = arrayOf("a", 1, null)
    assertEquals(arr.asList().toString(), arr.contentToString())
}
