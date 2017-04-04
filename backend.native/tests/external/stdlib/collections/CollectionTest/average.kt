import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    assertTrue { arrayListOf<Int>().average().isNaN() }
    expect(3.8) { listOf(1, 2, 5, 8, 3).average() }
    expect(2.1) { sequenceOf(1.6, 2.6, 3.6, 0.6).average() }
    expect(100.0) { arrayListOf<Byte>(100, 100, 100, 100, 100, 100).average() }
    val n = 100
    val range = 0..n
    expect(n.toDouble() / 2) { range.average() }
}
