// IGNORE_BACKEND_FIR: JVM_IR
class Box<T>(val value: T)

fun box() : String {
    val b = Box<Long>(2 * 3)
    val expected: Long? = 6L
    return if (b.value == expected) "OK" else "fail"
}