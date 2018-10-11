// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

inline class UInt(val x: Int)

inline class UIntArray(private val storage: IntArray) : Collection<UInt> {
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