// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

OPTIONAL_JVM_INLINE_ANNOTATION
value class SnekDirection(val direction: Int) {
    companion object {
        val Up = SnekDirection(0)
    }
}

fun testUnbox() : SnekDirection {
    val list = arrayListOf(SnekDirection.Up)
    return list[0]
}

fun box(): String {
    val a = testUnbox()
    return if (a.direction == 0) "OK" else "Fail"
}