// ISSUE: KT-71416
// FIR_IDENTICAL

// MODULE: lib
// FILE: A.kt
class A {
    private class Nested {
        fun foo() = "OK"
    }

    private <!NOTHING_TO_INLINE!>inline<!> fun privateFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION!>Nested()<!>.foo()
    internal <!NOTHING_TO_INLINE!>inline<!> fun internalInlineFun() = privateFun()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}