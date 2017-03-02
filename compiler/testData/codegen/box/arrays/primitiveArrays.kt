fun <T> eq(expected: T, actual: T): Boolean {
    return when (expected) {
        is ByteArray -> actual is ByteArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is ShortArray -> actual is ShortArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is CharArray -> actual is CharArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is IntArray -> actual is IntArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is LongArray -> actual is LongArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is FloatArray -> actual is FloatArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        is DoubleArray -> actual is DoubleArray && actual.size == expected.size && actual.foldIndexed(true) { i, r, v -> r && expected[i] == v }
        else -> false
    }
}

fun chv(vararg c: Char): CharArray {
    return c
}

fun box(): String {
    if (!eq(byteArrayOf(0), ByteArray(1))) return "fail 2"
    if (!eq(byteArrayOf(1), byteArrayOf(1).copyOf())) return "fail 2"
    if (!eq(byteArrayOf(1, 0), byteArrayOf(1).copyOf(2))) return "fail 5"
    if (!eq(byteArrayOf(1), byteArrayOf(1, 2).copyOf(1))) return "fail 9"

    if (!eq(charArrayOf('a', 'b', 'c'), chv('a', 'b', 'c'))) return "fail vararg"

    if (!eq(charArrayOf('a', 'b', 'c'), CharArray(3) { 'a' + it })) return "fail constructor"

    return "OK"
}