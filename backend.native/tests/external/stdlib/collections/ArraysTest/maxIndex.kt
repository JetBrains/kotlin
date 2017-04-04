import kotlin.test.*

fun box() {
    val a = intArrayOf(1, 7, 9, 239, 54, 93)
    expect(3, { a.indices.maxBy { a[it] } })
}
