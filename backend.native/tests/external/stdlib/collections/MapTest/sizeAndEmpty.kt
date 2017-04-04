import kotlin.test.*


fun box() {
    val data = hashMapOf<String, Int>()
    assertTrue { data.none() }
    assertEquals(data.size, 0)
}
