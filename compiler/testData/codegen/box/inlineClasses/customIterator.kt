// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_TEXT
// WITH_STDLIB
// KT-44529

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlineDouble3(val values: DoubleArray) {
    operator fun iterator(): DoubleIterator = IteratorImpl(values)
}

// This iterator returns the first 3 elements of this.values
private class IteratorImpl(private val values: DoubleArray) : DoubleIterator() {
    private var index = 0
    override fun hasNext(): Boolean = index < 3
    override fun nextDouble(): Double = values[index++]
}

fun box(): String {
    val values = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
    var result = ""
    for (i in InlineDouble3(values)) {
        result += i.toString().substring(0, 1)
    }
    return if (result == "123") "OK" else "Fail: $result"
}
