// LANGUAGE: +ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs
// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

private class A()

private class B(a: A)

private class C {
    internal class Nested(b: B)
}

internal inline fun internalFun(): Any = C.<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>Nested(<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!PRIVATE_CLASS_MEMBER_FROM_INLINE!>B<!>(<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!><!PRIVATE_CLASS_MEMBER_FROM_INLINE!>A<!>()<!>)<!>)<!>