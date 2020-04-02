// TARGET_BACKEND: JVM
// WITH_RUNTIME

object Host {
    @JvmStatic
    fun foo(s: String = "OK") = s
}

fun box(): String = Host.foo()