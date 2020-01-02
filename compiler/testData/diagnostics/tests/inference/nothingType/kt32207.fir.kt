// !DIAGNOSTICS: -UNUSED_PARAMETER

class Test<X, T> {
    fun hereIdeaFail(values : List<Int>, others : List<String>): List<Test<out Int, out String>> {
        return values.map { left(it) }.plus(others.map { right(it) })
    }

    companion object {
        fun <L> left(left: L): Test<L, Nothing> = TODO()
        fun <R> right(right: R): Test<Nothing, R> = TODO()
    }
}

fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R> = TODO()
operator fun <T> Collection<T>.plus(elements: Iterable<T>): List<T> = TODO()
