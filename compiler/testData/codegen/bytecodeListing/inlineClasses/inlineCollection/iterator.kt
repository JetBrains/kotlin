// IGNORE_ANNOTATIONS

inline class InlineIterator<T>(private val it: Iterator<T>) : Iterator<T> {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): T = it.next()
}

