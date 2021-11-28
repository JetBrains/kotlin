// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
public class Z {
    internal val privateProperty = 11;

    internal fun privateFun() {

    }
}

public inline fun test() {
    Z().<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateProperty<!>
    Z().<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()
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
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateProperty<!>
        <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>privateFun<!>()
    }

    internal inline fun testInternal() {
        privateProperty
        privateFun()
    }
}