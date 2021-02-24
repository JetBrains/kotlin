// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineMutableIterable(private val it: MutableIterable<IT>) : MutableIterable<IT> {
    override fun iterator(): MutableIterator<IT> = it.iterator()
}

