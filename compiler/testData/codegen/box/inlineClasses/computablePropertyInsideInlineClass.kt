// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class UIntArray(private val intArray: IntArray) {
    val size get() = intArray.size
}

fun box(): String {
    val array = UIntArray(intArrayOf(1, 2, 3))
    return if (array.size != 3) "fail" else "OK"
}