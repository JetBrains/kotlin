// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

open class A

private class B : A()

internal inline fun inlineFun(): A {
    return (<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>A() as B<!>)
}
