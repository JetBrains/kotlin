// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_VARIABLE

private class C

private inline fun simple() {
    val c: C? = null
}

private inline fun generic() {
    // Cannot detect this usage of C because there's only an erased type in LocalVariableTable.
    val c: List<C>? = null
}

internal inline fun test() {
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>simple()<!>
    generic()
}
