import kotlin.test.*

fun box() {
    assertEquals(listOf(2), arrayOf("a", null, "test").mapIndexedNotNull { index, it -> it?.run { if (index != 0) length / index else null } })
}
