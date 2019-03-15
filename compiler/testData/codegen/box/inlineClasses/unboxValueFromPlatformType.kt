// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR, NATIVE

inline class SnekDirection(val direction: Int) {
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