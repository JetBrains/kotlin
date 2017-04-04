import kotlin.test.*

fun box() {
    expect(null, { arrayOf<Int>().min() })
    expect(1, { arrayOf(1).min() })
    expect(2, { arrayOf(2, 3).min() })
    expect(2000000000000, { arrayOf(3000000000000, 2000000000000).min() })
    expect('a', { arrayOf('a', 'b').min() })
    expect("a", { arrayOf("a", "b").min() })
}
