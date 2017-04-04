import kotlin.test.*

fun box() {
    expect(3) { arrayOf(1, 2, 3).last() }
    expect(2) { arrayOf(1, 2, 3).last { it % 2 == 0 } }
}
