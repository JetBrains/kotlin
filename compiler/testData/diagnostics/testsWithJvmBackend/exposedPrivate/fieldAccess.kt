// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE
// WITH_STDLIB

private class C

class Test {
    private companion object {
        @JvmField
        var privateField: C? = C()
    }

    private inline fun produce() { privateField }
    private inline fun consume() { privateField = null }

    internal inline fun test() {
        <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>produce()<!>
        <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING, PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>consume()<!>
    }
}
