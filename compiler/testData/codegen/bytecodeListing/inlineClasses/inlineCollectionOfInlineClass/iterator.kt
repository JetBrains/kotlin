// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineIterator(private val it: Iterator<IT>) : Iterator<IT> {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): IT = it.next()
}
