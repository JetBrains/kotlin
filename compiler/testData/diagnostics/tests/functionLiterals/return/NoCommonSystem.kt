// FIR_IDENTICAL
fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!
fun <T> listOf(): List<T> = null!!
fun <T> listOf(vararg values: T): List<T> = null!!

fun commonSystemFailed(a: List<Int>) {
    a.map {
        if (it == 0) return@map listOf(it)
        listOf()
    }
    a.map {
        if (it == 0) return@map listOf()
        listOf(it)
    }
    a.map {
        if (it == 0) listOf()
        else listOf(it)
    }
}
