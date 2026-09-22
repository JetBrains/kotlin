// TARGET_BACKEND: JVM
// WITH_STDLIB

fun box(): String {
    class Bean {
        @JvmField
        val a: String = "OK"
    }

    return Bean().a
}
