// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

private fun interface I {
    fun foo(): Int
}

inline fun publicInlineFun(): Int = (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>I { 1 }<!>).<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>()

internal inline fun internalInlineFun(): Int = (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>I { 1 }<!>).<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()
