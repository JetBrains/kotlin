import kotlin.test.*

fun box() {
    var c = 0
    expect(1, { arrayOf(5, 4, 3, 2, 1).minBy { c++; it * it } })
    assertEquals(5, c)
}
