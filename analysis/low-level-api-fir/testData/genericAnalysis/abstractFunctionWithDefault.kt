// ISSUE: KT-85864
abstract class F<T> {
    abstract fun f(x: T, a: Int = 0): Int
}

inline fun <T, R> Iterable<T>.myMap(transform: (T) -> R): List<R> = null!!

abstract class G<T> : F<T>() {
    fun g(): List<Int> {
        val e: List<T> = null!!
        return e.myMap(::f)
    }
}
