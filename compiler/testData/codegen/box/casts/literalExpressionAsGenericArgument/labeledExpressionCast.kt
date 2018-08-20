// IGNORE_BACKEND: JS_IR
class Box<T>(val value: T)

fun box() : String {
    val b = Box<Long>(x@ (1 + 2))
    val expected: Long? = 3L
    return if (b.value == expected) "OK" else "fail"
}