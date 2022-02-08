// WITH_STDLIB

// FILE: main.kt
import kotlin.reflect.KFunction2

class Sample {
    object A {
        fun max(x: Int, y: Int): Int {
            return if (x > y) {
                x
            } else {
                y
            }
        }
    }
}

object A2 {
    fun max(x: Int, y: Int): Int {
        return if (x > y) {
            x
        } else {
            y
        }
    }
}

abstract class Checker {
    fun check(): String {
        return run(
            Sample.A::max,
            { x, y -> x > y }
        )
    }
    fun check2(): String {
        return run(
            A2::max,
            { x, y -> x > y }
        )
    }
    abstract fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String
}

fun box(): String {
    var result = ( object : Checker() {
        override fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String {
            return "O"
        }
    } ).check()
    result += ( object : Checker() {
        override fun <T1, T2, R> run(method: KFunction2<T1, T2, R>, fn: (T1, T2) -> Boolean): String {
            return "K"
        }
    } ).check2()

    return result
}
