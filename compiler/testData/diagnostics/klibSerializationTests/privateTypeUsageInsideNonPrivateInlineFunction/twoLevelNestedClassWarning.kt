// LANGUAGE: -ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class A {
    class N1 {
        class N2
    }
}

internal inline fun inlineFun(): Any = A.<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>N1<!>.<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!><!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_WARNING!>N2<!>()<!>
