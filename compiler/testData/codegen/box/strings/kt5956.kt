// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// KT-5956 java.lang.AbstractMethodError: test.Thing.subSequence(II)Ljava/lang/CharSequence

class Thing(val delegate: CharSequence) : CharSequence {
    override fun get(index: Int): Char {
        throw UnsupportedOperationException()
    }
    override val length: Int get() = 0
    override fun subSequence(start: Int, end: Int) = delegate.subSequence(start, end)
}

fun box(): String {
    val txt = Thing("hello there")
    val s = txt.subSequence(0, 1)
    return if ("$s" == "h") "OK" else "Fail: $s"
}
