import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(null, { listOf<Int>().min() })
    expect(1, { listOf(1).min() })
    expect(2, { listOf(2, 3).min() })
    expect(2000000000000, { listOf(3000000000000, 2000000000000).min() })
    expect('a', { listOf('a', 'b').min() })
    expect("a", { listOf("a", "b").min() })
    expect(null, { listOf<Int>().asSequence().min() })
    expect(2, { listOf(2, 3).asSequence().min() })
}
