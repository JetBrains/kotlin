// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
private class Z public (){
    public val publicProperty:Int = 12
    public fun publicFun() {}
}

public inline fun test() {
    <!INVISIBLE_MEMBER_FROM_INLINE!>Z<!>().<!INVISIBLE_MEMBER_FROM_INLINE!>publicProperty<!>
    <!INVISIBLE_MEMBER_FROM_INLINE!>Z<!>().<!INVISIBLE_MEMBER_FROM_INLINE!>publicFun<!>()
}

inline fun testInternal() {
    Z().publicProperty
    Z().publicFun()
}

private class Z2 {
    private val privateProperty = 11;

    public val publicProperty:Int = 12

    private fun privateFun() {}

    public fun publicFun() {}

    public inline fun test() {
        privateProperty
        privateFun()
        publicProperty
        publicFun()
        Z2().publicProperty
        Z2().publicFun()
        Z2().privateProperty
        Z2().privateFun()
    }

    inline fun testInternal() {
        privateProperty
        privateFun()
        publicProperty
        publicFun()
        Z2().publicProperty
        Z2().publicFun()
        Z2().privateProperty
        Z2().privateFun()
    }
}