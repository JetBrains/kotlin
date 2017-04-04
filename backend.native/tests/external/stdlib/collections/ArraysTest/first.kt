import kotlin.test.*

fun box() {
    expect(1) { arrayOf(1, 2, 3).first() }
    expect(2) { arrayOf(1, 2, 3).first { it % 2 == 0 } }
}
