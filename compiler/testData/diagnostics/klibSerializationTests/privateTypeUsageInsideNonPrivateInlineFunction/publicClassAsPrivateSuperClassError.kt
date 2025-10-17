// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE

open class A

private class B : A()

internal inline fun inlineFun(): A {
    return (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>A() as <!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING!>B<!><!>)
}
