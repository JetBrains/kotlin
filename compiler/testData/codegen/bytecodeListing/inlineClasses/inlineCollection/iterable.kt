// IGNORE_ANNOTATIONS

inline class InlineIterable<T>(private val it: Iterable<T>) : Iterable<T> {
    override fun iterator(): Iterator<T> = it.iterator()
}

