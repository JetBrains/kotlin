import kotlin.test.*


fun box() {
    val data = listOf("foo", "bar", "whatnot")
    val actual = data.drop(1)
    assertEquals(listOf("bar", "whatnot"), actual)
}
