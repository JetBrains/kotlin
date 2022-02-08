// WITH_STDLIB
// TARGET_BACKEND: JVM
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class UIntArray(private val storage: IntArray) : Collection<UInt> {
    public override val size: Int get() = storage.size

    override operator fun iterator() = TODO()
    override fun contains(element: UInt): Boolean = TODO()
    override fun containsAll(elements: Collection<UInt>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
}

fun calculate(u: UIntArray): Int {
    return u.size
}

fun box(): String {
    if (calculate(UIntArray(intArrayOf(1, 2, 3, 4))) != 4) return "Fail"
    return "OK"
}