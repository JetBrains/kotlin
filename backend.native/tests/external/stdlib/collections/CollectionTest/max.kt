import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(null, { listOf<Int>().max() })
    expect(1, { listOf(1).max() })
    expect(3, { listOf(2, 3).max() })
    expect(3000000000000, { listOf(3000000000000, 2000000000000).max() })
    expect('b', { listOf('a', 'b').max() })
    expect("b", { listOf("a", "b").max() })
    expect(null, { listOf<Int>().asSequence().max() })
    expect(3, { listOf(2, 3).asSequence().max() })
}
