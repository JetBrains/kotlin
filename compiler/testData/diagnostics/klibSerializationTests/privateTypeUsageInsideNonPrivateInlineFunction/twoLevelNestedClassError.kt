import A.N1.N2

// DIAGNOSTICS: -NOTHING_TO_INLINE
// FIR_IDENTICAL

private class A {
    open class N1 {
        public class N2
    }
}

internal inline fun inlineFun1(): Any = A.N1.<!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>N2()<!>
