// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// MODULE: lib
// FILE: A.kt
class A {
    private class Nested {
        fun foo() = "OK"
    }

    private inline fun privateFun() = Nested().foo()
    internal inline fun internalInlineFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING_ERROR!>privateFun()<!>
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return "OK" // Callsite of `A().internalInlineFun()` is omitted, to avoid test pipeline crash in public inliner after error diagnostic in dependent module
}
