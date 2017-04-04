import kotlin.test.*
import kotlin.comparisons.*

fun box() {
    expect(0) { arrayListOf<Int>().sum() }
    expect(14) { listOf(2, 3, 9).sum() }
    expect(3.0) { listOf(1.0, 2.0).sum() }
    expect(3000000000000) { arrayListOf<Long>(1000000000000, 2000000000000).sum() }
    expect(3.0.toFloat()) { arrayListOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
    expect(3.0.toFloat()) { sequenceOf<Float>(1.0.toFloat(), 2.0.toFloat()).sum() }
}
