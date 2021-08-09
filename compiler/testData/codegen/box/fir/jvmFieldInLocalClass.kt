// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME

fun box(): String {
    class Bean {
        @JvmField
        val a: String = "OK"
    }

    return Bean().a
}
