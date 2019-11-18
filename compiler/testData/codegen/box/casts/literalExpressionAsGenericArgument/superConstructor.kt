// IGNORE_BACKEND_FIR: JVM_IR
open class Base<T>(val value: T)
class Box(): Base<Long>(-1)

fun box(): String {
    val expected: Long? = -1L
    return if (Box().value == expected) "OK" else "fail"
}