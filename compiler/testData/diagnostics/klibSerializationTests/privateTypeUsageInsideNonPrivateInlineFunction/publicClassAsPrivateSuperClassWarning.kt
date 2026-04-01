// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

open class A

private class B : A()

internal inline fun inlineFun(): A {
    return (A() as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>B<!>)
}
