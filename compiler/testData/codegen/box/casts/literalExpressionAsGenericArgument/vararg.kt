// IGNORE_BACKEND_FIR: JVM_IR
class Box<T>(val value: T)

fun <T> run(vararg z: T): Box<T> {
    return Box<T>(z[0])
}

fun box(): String {
    val b = run<Long>(-1, -1, -1)
    val expected: Long? = -1L
    return if (b.value == expected) "OK" else "fail"
}