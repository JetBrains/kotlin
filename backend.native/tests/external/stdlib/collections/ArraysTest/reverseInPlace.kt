import kotlin.test.*

fun box() {

    fun <TArray, T> doTest(build: Iterable<Int>.() -> TArray, reverse: TArray.() -> Unit, snapshot: TArray.() -> List<T>) {
        val arrays = (0..4).map { n -> (1..n).build() }
        for (array in arrays) {
            val original = array.snapshot()
            array.reverse()
            val reversed = array.snapshot()
            assertEquals(original.asReversed(), reversed)
        }
    }

    doTest(build = { map { it }.toIntArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toLong() }.toLongArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toByte() }.toByteArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toShort() }.toShortArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toFloat() }.toFloatArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toDouble() }.toDoubleArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { 'a' + it }.toCharArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it % 2 == 0 }.toBooleanArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toString() }.toTypedArray() }, reverse = { reverse() }, snapshot = { toList() })
    doTest(build = { map { it.toString() }.toTypedArray() as Array<out String> }, reverse = { reverse() }, snapshot = { toList() })
}
