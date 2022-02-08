// WITH_STDLIB

// FILE: main.kt
import kotlin.reflect.KFunction1

class Sample {
    inner class SS
}

abstract class Checker {
    fun check(): String {
        return run(
            Sample::SS,
            { x, y -> x == Any() && y == Any() }
        )
    }
    abstract fun <T1, T2> run(method: KFunction1<T1, T2>, fn: (T1, T2) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1, T2> run(method: KFunction1<T1, T2>, fn: (T1, T2) -> Boolean): String {
            return "OK"
        }
    } ).check()

    return result
}
