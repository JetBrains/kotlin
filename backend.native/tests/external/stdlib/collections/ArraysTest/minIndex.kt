import kotlin.test.*

fun box() {
    val a = intArrayOf(1, 7, 9, -42, 54, 93)
    expect(3, { a.indices.minBy { a[it] } })
}
