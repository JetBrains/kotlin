// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private context(c: C?) fun contextC() { c }

private inline fun consumeWithContext() { with(null) { contextC() } }

internal inline fun test() {
    <!PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR!>consumeWithContext()<!>
}
