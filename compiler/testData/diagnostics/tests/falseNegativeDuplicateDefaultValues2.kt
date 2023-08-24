// FIR_IDENTICAL
// ISSUE: KT-36188

interface SomeRandomBase<K> {
    fun child(props: Int = 20)
}

interface SomeRandomOverride<J> : SomeRandomBase<J> {
    override fun child(props: Int) {}
}

open class Keker<P> {
    open fun child(props: Int = 10) {}
}

class Implementation<P>() : Keker<P>(), SomeRandomOverride<P> {
    override fun child(props: Int) {}
}
