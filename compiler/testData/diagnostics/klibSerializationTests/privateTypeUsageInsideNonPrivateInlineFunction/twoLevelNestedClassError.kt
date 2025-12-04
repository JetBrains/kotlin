// RUN_PIPELINE_TILL: FRONTEND
import A.N1.N2

// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class A {
    open class N1 {
        public class N2
    }
}

internal inline fun inlineFun1(): Any = A.<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>N1<!>.<!LESS_VISIBLE_TYPE_IN_INLINE_ACCESSED_SIGNATURE_ERROR!>N2<!>()
