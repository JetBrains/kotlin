// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

private class Private {
    fun foo() = "OK"
}

internal inline fun internalInlineFun(): String {
    return <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!><!PRIVATE_CLASS_MEMBER_FROM_INLINE!>Private<!>()<!>.<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()
}
