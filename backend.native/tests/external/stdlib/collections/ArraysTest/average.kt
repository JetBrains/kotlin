import kotlin.test.*

fun box() {
    assertTrue() { arrayOf<Int>().average().isNaN() }
    expect(3.8) { arrayOf(1, 2, 5, 8, 3).average() }
    expect(2.1) { arrayOf(1.6, 2.6, 3.6, 0.6).average() }
    expect(100.0) { arrayOf<Byte>(100, 100, 100, 100, 100, 100).average() }
    expect(0) { arrayOf<Short>(1, -1, 2, -2, 3, -3).average().toShort() }
    // TODO: Property based tests
    // for each arr with size 1 arr.average() == arr[0]
    // for each arr with size > 0  arr.average() = arr.sum().toDouble() / arr.size()
}
