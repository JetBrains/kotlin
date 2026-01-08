// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private inline fun privateFun() = C()

internal open class A {
    protected inline fun test() {
        <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>privateFun()<!>
    }
}
