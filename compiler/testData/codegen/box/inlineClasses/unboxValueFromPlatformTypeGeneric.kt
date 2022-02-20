// WITH_STDLIB
// IGNORE_BACKEND: NATIVE
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class SnekDirection<T: Int>(val direction: T) {
    companion object {
        val Up = SnekDirection(0)
    }
}

fun testUnbox() : SnekDirection<Int> {
    val list = arrayListOf(SnekDirection.Up)
    return list[0]
}

fun box(): String {
    val a = testUnbox()
    return if (a.direction == 0) "OK" else "Fail"
}