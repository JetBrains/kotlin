import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().maxWith(naturalOrder()) })
    expect(1, { intArrayOf(1).maxWith(naturalOrder()) })
    expect(-4, { intArrayOf(2, 3, -4).maxWith(compareBy { it * it }) })
}
