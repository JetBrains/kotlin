// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class UIntArray(private val intArray: IntArray) {
    val size get() = intArray.size
}

fun box(): String {
    val array = UIntArray(intArrayOf(1, 2, 3))
    return if (array.size != 3) "fail" else "OK"
}