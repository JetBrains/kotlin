// WITH_STDLIB
// IGNORE_BACKEND: NATIVE

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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