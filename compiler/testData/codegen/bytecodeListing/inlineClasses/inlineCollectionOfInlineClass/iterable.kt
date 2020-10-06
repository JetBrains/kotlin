// IGNORE_ANNOTATIONS

inline class IT(val x: Int)

inline class InlineIterable(private val it: Iterable<IT>) : Iterable<IT> {
    override fun iterator(): Iterator<IT> = it.iterator()
}
