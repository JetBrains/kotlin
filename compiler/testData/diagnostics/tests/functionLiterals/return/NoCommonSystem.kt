// !WITH_NEW_INFERENCE
fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = null!!
fun <T> listOf(): List<T> = null!!
fun <T> listOf(vararg values: T): List<T> = null!!

fun commonSystemFailed(a: List<Int>) {
    a.map {
        if (it == 0) return@map listOf(it)
        <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>listOf<!>()
    }
    a.map {
        if (it == 0) return@map <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>listOf<!>()
        listOf(it)
    }
    a.map {
        if (it == 0) listOf()
        else listOf(it)
    }
}
