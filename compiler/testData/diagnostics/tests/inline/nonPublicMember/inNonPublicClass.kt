// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE -PRIVATE_CLASS_MEMBER_FROM_INLINE
private class Z public constructor(){
    public val publicProperty:Int = 12
    public fun publicFun() {}
}

public inline fun test() {
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Z<!>().<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>publicProperty<!>
    <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Z<!>().<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>publicFun<!>()
}

internal inline fun testInternal() {
    Z().publicProperty
    Z().publicFun()
}

internal class Z2 {
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

    internal inline fun testInternal() {
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