import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(null, { listOf<Int>().maxBy { it } })
    expect(1, { listOf(1).maxBy { it } })
    expect(2, { listOf(2, 3).maxBy { -it } })
    expect('b', { listOf('a', 'b').maxBy { "x$it" } })
    expect("abc", { listOf("b", "abc").maxBy { it.length } })
    expect(null, { listOf<Int>().asSequence().maxBy { it } })
    expect(2, { listOf(2, 3).asSequence().maxBy { -it } })
}
