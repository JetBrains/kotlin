// TARGET_BACKEND: JVM
// WITH_STDLIB

class Host {
    companion object {
        @JvmStatic
        fun foo(s: String = "OK") = s
    }
}

fun box(): String = Host.foo()