// WITH_STDLIB
inline class InlineCharSequence(private val cs: CharSequence) : CharSequence {
    override val length: Int get() = cs.length
    override fun get(index: Int): Char = cs[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = cs.subSequence(startIndex, endIndex)
}