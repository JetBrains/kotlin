import kotlin.test.*

fun box() {
    expect(null, { arrayOf<Int>().maxBy { it } })
    expect(1, { arrayOf(1).maxBy { it } })
    expect(2, { arrayOf(2, 3).maxBy { -it } })
    expect('b', { arrayOf('a', 'b').maxBy { "x$it" } })
    expect("abc", { arrayOf("b", "abc").maxBy { it.length } })
}
