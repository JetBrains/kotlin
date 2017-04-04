import kotlin.test.*



fun box() {
    val data = "abedcf"
    assertEquals("abdf", data.filterIndexed { index, c -> c == 'a' + index })
}
