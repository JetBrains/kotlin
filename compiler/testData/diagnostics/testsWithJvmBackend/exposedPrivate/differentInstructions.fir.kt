// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_EXPRESSION
// IGNORE_FIR_DIAGNOSTICS
// WITH_STDLIB

private class C : Throwable() {
    companion object {
        @JvmField
        var staticField = Any()
    }
}

private inline fun ldc() { C::class }
private inline fun <reified T> ldcReified() { T::class }
private inline fun newarray() { emptyArray<C>() }
private inline fun checkcast(x: Any?) { x as C? }
private inline fun instanceof(x: Any?) { x is C? }
private inline fun getstatic() {
    C.staticField
}
private inline fun putstatic() {
    C.staticField = Any()
}
private inline fun tryCatch() {
    try {} catch (c: C) {}
}

internal inline fun test() {
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>ldc()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>ldcReified<<!LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_ERROR!>C<!>>()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>newarray()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>checkcast(null)<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>instanceof(null)<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>getstatic()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>putstatic()<!>
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>tryCatch()<!>
}
