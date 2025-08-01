// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

class C {
    private companion object {
        fun foo() = "OK"
    }

    private inline fun privateFun() = foo()
    internal inline fun test() = <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>privateFun()<!>
}
