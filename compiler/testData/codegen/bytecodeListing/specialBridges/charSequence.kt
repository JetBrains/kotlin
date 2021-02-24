abstract class AbstractCharSequence : CharSequence

class MyCharSequence : CharSequence {
    override val length: Int get() = 0

    override fun get(index: Int): Char = throw Exception()

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = this
}