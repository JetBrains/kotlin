// IGNORE_BACKEND_FIR: JVM_IR
class Box<T>(val value: T)

fun box() : String {
    val b = Box<Long>(-1)
    val expected: Long? = -1L
    return if (b.value == expected) "OK" else "fail"
}