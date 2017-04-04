import kotlin.test.*

fun box() {
    expect(-1) { byteArrayOf(1, 2, 3).indexOf(0) }
    expect(0) { byteArrayOf(1, 2, 3).indexOf(1) }
    expect(1) { byteArrayOf(1, 2, 3).indexOf(2) }
    expect(2) { byteArrayOf(1, 2, 3).indexOf(3) }

    expect(-1) { shortArrayOf(1, 2, 3).indexOf(0) }
    expect(0) { shortArrayOf(1, 2, 3).indexOf(1) }
    expect(1) { shortArrayOf(1, 2, 3).indexOf(2) }
    expect(2) { shortArrayOf(1, 2, 3).indexOf(3) }

    expect(-1) { intArrayOf(1, 2, 3).indexOf(0) }
    expect(0) { intArrayOf(1, 2, 3).indexOf(1) }
    expect(1) { intArrayOf(1, 2, 3).indexOf(2) }
    expect(2) { intArrayOf(1, 2, 3).indexOf(3) }

    expect(-1) { longArrayOf(1, 2, 3).indexOf(0) }
    expect(0) { longArrayOf(1, 2, 3).indexOf(1) }
    expect(1) { longArrayOf(1, 2, 3).indexOf(2) }
    expect(2) { longArrayOf(1, 2, 3).indexOf(3) }

    expect(-1) { floatArrayOf(1.0f, 2.0f, 3.0f).indexOf(0f) }
    expect(0) { floatArrayOf(1.0f, 2.0f, 3.0f).indexOf(1.0f) }
    expect(1) { floatArrayOf(1.0f, 2.0f, 3.0f).indexOf(2.0f) }
    expect(2) { floatArrayOf(1.0f, 2.0f, 3.0f).indexOf(3.0f) }

    expect(-1) { doubleArrayOf(1.0, 2.0, 3.0).indexOf(0.0) }
    expect(0) { doubleArrayOf(1.0, 2.0, 3.0).indexOf(1.0) }
    expect(1) { doubleArrayOf(1.0, 2.0, 3.0).indexOf(2.0) }
    expect(2) { doubleArrayOf(1.0, 2.0, 3.0).indexOf(3.0) }

    expect(-1) { charArrayOf('a', 'b', 'c').indexOf('z') }
    expect(0) { charArrayOf('a', 'b', 'c').indexOf('a') }
    expect(1) { charArrayOf('a', 'b', 'c').indexOf('b') }
    expect(2) { charArrayOf('a', 'b', 'c').indexOf('c') }

    expect(0) { booleanArrayOf(true, false).indexOf(true) }
    expect(1) { booleanArrayOf(true, false).indexOf(false) }
    expect(-1) { booleanArrayOf(true).indexOf(false) }
}
