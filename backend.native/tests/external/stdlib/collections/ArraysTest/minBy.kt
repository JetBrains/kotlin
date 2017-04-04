import kotlin.test.*

fun box() {
    expect(null, { arrayOf<Int>().minBy { it } })
    expect(1, { arrayOf(1).minBy { it } })
    expect(3, { arrayOf(2, 3).minBy { -it } })
    expect('a', { arrayOf('a', 'b').minBy { "x$it" } })
    expect("b", { arrayOf("b", "abc").minBy { it.length } })
}
