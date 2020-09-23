class Box<T>(val value: T)

fun box() : String {
    val b = Box<Long>(2L * 3)
    val expected: Long? = 6L
    return if (b.value == expected) "OK" else "fail"
}
