// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER
// KT-7440 Cannot complete type inference if two extension functions for interface hierarchy

package inferenceagain

interface Base<T>
interface Derived<T> : Base<T>

fun <R : Comparable<R>, T : Any> Base<T>.maxBy(f: (T) -> R): T? = null
fun <R : Comparable<R>, T : Any> Derived<T>.maxBy(f: (T) -> R): T? = null

fun <T> derivedOf(vararg members: T): Derived<T> = null!!

fun <T> x(l: Derived<T>) {
    derivedOf(1, 2, 3).maxBy<Int, Int> { it  } // works
    derivedOf(1, 2, 3).maxBy { it } // should work
}