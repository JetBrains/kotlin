// TARGET_BACKEND: JVM
// WITH_RUNTIME

object A {
    @JvmStatic
    fun f(x: String = value()) = x

    fun value() = "OK"
}

fun box(): String {
    return A.f()
}
