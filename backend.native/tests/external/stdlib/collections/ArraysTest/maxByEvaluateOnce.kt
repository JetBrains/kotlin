import kotlin.test.*

fun box() {
    var c = 0
    expect(5, { arrayOf(5, 4, 3, 2, 1).maxBy { c++; it * it } })
    assertEquals(5, c)
}
