// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_EXPRESSION

private class C

private fun C.extension() { this }

private inline fun extensionReceiver() {  C().extension()  }

internal inline fun test() {
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>extensionReceiver()<!>
}
