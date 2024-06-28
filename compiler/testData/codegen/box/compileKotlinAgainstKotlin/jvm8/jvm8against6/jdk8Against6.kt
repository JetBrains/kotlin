// TARGET_BACKEND: JVM
// SKIP_JDK6
// FULL_JDK
// MODULE: lib
// FILE: A.kt
import java.util.*
class Jdk6List<F> : AbstractList<F>() {
    override fun get(index: Int): F {
        return "OK" as F
    }

    override val size: Int
        get() = 2

}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    val result = Jdk6List<String>().stream().filter { it == "OK" }.count()
    if (result != 2L) return "fai1: $result"

    return "OK"
}
