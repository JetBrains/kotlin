import kotlin.test.*

fun box() {
    expect(null, { arrayOf<Int>().max() })
    expect(1, { arrayOf(1).max() })
    expect(3, { arrayOf(2, 3).max() })
    expect(3000000000000, { arrayOf(3000000000000, 2000000000000).max() })
    expect('b', { arrayOf('a', 'b').max() })
    expect("b", { arrayOf("a", "b").max() })
}
