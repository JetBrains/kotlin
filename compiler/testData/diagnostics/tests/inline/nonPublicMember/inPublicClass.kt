// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
public class Z {
    internal val privateProperty = 11;

    internal fun privateFun() {

    }
}

public inline fun test() {
    Z().<!INVISIBLE_MEMBER_FROM_INLINE!>privateProperty<!>
    Z().<!INVISIBLE_MEMBER_FROM_INLINE!>privateFun<!>()
}

internal inline fun testInternal() {
    Z().privateProperty
    Z().privateFun()
}


public class Z2 {
    private val privateProperty = 11;

    private fun privateFun() {

    }

    public inline fun test() {
        <!INVISIBLE_MEMBER_FROM_INLINE!>privateProperty<!>
        <!INVISIBLE_MEMBER_FROM_INLINE!>privateFun<!>()
    }

    internal inline fun testInternal() {
        privateProperty
        privateFun()
    }
}