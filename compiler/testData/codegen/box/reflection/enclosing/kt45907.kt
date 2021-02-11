// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_RUNTIME

fun box(): String = use {
    class Local(val n: Int)
    if (Local::class.java.declaringClass == null) "OK" else "Fail"
}

inline fun <T> use(block: () -> T): T = block()
