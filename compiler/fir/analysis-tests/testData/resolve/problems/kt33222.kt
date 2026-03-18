// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-33222
// WITH_STDLIB

// KT-33222: Editing hangs while entering arguments to highly overloaded Java method
// Simulate assertj's assertThat which has ~87 overloads

class Assertion<T>(val value: T)

object Assert {
    fun assertThat(value: Byte): Assertion<Byte> = Assertion(value)
    fun assertThat(value: Short): Assertion<Short> = Assertion(value)
    fun assertThat(value: Int): Assertion<Int> = Assertion(value)
    fun assertThat(value: Long): Assertion<Long> = Assertion(value)
    fun assertThat(value: Float): Assertion<Float> = Assertion(value)
    fun assertThat(value: Double): Assertion<Double> = Assertion(value)
    fun assertThat(value: Boolean): Assertion<Boolean> = Assertion(value)
    fun assertThat(value: Char): Assertion<Char> = Assertion(value)
    fun assertThat(value: String): Assertion<String> = Assertion(value)
    fun assertThat(value: ByteArray): Assertion<ByteArray> = Assertion(value)
    fun assertThat(value: ShortArray): Assertion<ShortArray> = Assertion(value)
    fun assertThat(value: IntArray): Assertion<IntArray> = Assertion(value)
    fun assertThat(value: LongArray): Assertion<LongArray> = Assertion(value)
    fun assertThat(value: FloatArray): Assertion<FloatArray> = Assertion(value)
    fun assertThat(value: DoubleArray): Assertion<DoubleArray> = Assertion(value)
    fun assertThat(value: BooleanArray): Assertion<BooleanArray> = Assertion(value)
    fun assertThat(value: CharArray): Assertion<CharArray> = Assertion(value)
    fun assertThat(value: Array<String>): Assertion<Array<String>> = Assertion(value)
    fun assertThat(value: List<String>): Assertion<List<String>> = Assertion(value)
    fun assertThat(value: Set<String>): Assertion<Set<String>> = Assertion(value)
    fun assertThat(value: Map<String, String>): Assertion<Map<String, String>> = Assertion(value)
    fun <T> assertThat(value: Iterable<T>): Assertion<Iterable<T>> = Assertion(value)
    fun <T> assertThat(value: Comparable<T>): Assertion<Comparable<T>> = Assertion(value)
    fun assertThat(value: CharSequence): Assertion<CharSequence> = Assertion(value)
    fun assertThat(value: Number): Assertion<Number> = Assertion(value)
    fun assertThat(value: Any?): Assertion<Any?> = Assertion(value)
}

class MyTest {
    fun test() {
        with(Assert) {
            assertThat(42)
            assertThat("hello")
            assertThat(3.14)
            assertThat(true)
            assertThat(intArrayOf(1, 2, 3))
            assertThat(listOf("a", "b"))
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, lambdaLiteral, nullableType,
objectDeclaration, primaryConstructor, propertyDeclaration, stringLiteral, typeParameter */
