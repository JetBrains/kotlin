// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class UInt<T: Int>(private val value: T) {
    fun asInt() = value
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class UIntArray(private val intArray: IntArray) {
    operator fun get(index: Int): UInt<Int> = UInt<Int>(intArray[index])

    operator fun set(index: Int, value: UInt<Int>) {
        intArray[index] = value.asInt()
    }
}

fun UIntArray.swap(i: Int, j: Int) {
    this[j] = this[i].also { this[i] = this[j] }
}

fun uIntArrayOf(vararg elements: Int) = UIntArray(intArrayOf(*elements))

fun box(): String {
    val a = uIntArrayOf(1, 2, 3, 4)
    a.swap(0, 3)
    a.swap(1, 2)

    if (a[0].asInt() != 4) return "fail"
    if (a[1].asInt() != 3) return "fail"
    if (a[2].asInt() != 2) return "fail"
    if (a[3].asInt() != 1) return "fail"

    return "OK"
}