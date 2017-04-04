import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(null, { listOf<Int>().minBy { it } })
    expect(1, { listOf(1).minBy { it } })
    expect(3, { listOf(2, 3).minBy { -it } })
    expect('a', { listOf('a', 'b').minBy { "x$it" } })
    expect("b", { listOf("b", "abc").minBy { it.length } })
    expect(null, { listOf<Int>().asSequence().minBy { it } })
    expect(3, { listOf(2, 3).asSequence().minBy { -it } })
}
