// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_PARAMETER

private class C {
    fun privateClassMember() {}
}

private fun produceC(): C = C()
private fun consumeC(c: C?) {}

private inline fun produceOnly() { produceC() }
private inline fun produceAndCallMember() { produceC().privateClassMember() }
private inline fun produceAndConsume() { consumeC(produceC()) }
private inline fun consumeOnly() { consumeC(null) }

internal inline fun test() {
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>produceOnly()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>produceAndCallMember()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>produceAndConsume()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>consumeOnly()<!>
}
