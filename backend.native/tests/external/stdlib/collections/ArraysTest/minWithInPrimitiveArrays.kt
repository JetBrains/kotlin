import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().minWith(naturalOrder()) })
    expect(1, { intArrayOf(1).minWith(naturalOrder()) })
    expect(4, { intArrayOf(2, 3, 4).minWith(compareBy { it % 4 }) })
}
