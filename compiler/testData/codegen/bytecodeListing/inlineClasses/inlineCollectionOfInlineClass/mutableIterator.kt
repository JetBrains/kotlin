// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineMutableIterator(private val it: MutableIterator<IT>) : MutableIterator<IT> {
    override fun hasNext(): Boolean = it.hasNext()
    override fun next(): IT = it.next()
    override fun remove() { it.remove() }
}
