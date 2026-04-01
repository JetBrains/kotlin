// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class A {
    class N1 {
        class N2
    }
}

internal inline fun inlineFun(): Any = A.<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>N1<!>.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>N2<!>()
