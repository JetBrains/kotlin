// DONT_TARGET_EXACT_BACKEND: JS_IR
// KT-74384: When upgradeCallableReferencesPhase will run for JS_IR, source locations will be corrected

// LANGUAGE: -ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

private fun interface I {
    fun foo(): Int
}

inline fun publicInlineFun(): Int = (I <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>{ 1 }<!>).<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>foo<!>()

internal inline fun internalInlineFun(): Int = (I <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>{ 1 }<!>).<!PRIVATE_CLASS_MEMBER_FROM_INLINE!>foo<!>()
