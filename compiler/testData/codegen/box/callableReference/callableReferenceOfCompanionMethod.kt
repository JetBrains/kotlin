// WITH_STDLIB

// FILE: main.kt
import kotlin.reflect.KFunction2

class Sample {
    companion object {
        fun max(x: Int, y: Int): Int {
            return if (x > y) {
                x
            } else {
                y
            }
        }
    }
}

abstract class Checker {
    fun check(): String {
        return run(
            Sample::max,
            { x, y -> x > y }
        )
    }
    abstract fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String {
            return "OK"
        }
    } ).check()

    return result
}
