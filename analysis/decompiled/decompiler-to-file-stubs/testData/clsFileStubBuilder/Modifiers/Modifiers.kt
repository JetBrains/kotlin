// Issue: KTIJ-26788 (need stdlib for Continuation)
// KNM_K2_IGNORE
// KNM_FE10_IGNORE

package test
import kotlin.coroutines.*

data class Modifiers(val x: Int) {
    external fun extFun()

    var extVar: Int = 1
        external get
        external set

    tailrec fun sum(x: Long, sum: Long): Long {
        if (x == 0.toLong()) return sum
        return sum(x - 1, sum + x)
    }

    inline fun inlined(crossinline arg1: ()->Unit, noinline arg2: ()->Unit): Unit {}

    override operator fun equals(other: Any?) = false

    annotation class Ann

    suspend fun suspend(x: Continuation<Int>) {}

    fun builder(c: suspend Any.() -> Unit) {}
}
