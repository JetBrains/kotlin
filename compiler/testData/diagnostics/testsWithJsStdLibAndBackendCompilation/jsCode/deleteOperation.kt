// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Any) {
    js("delete x.foo;")
    js("delete x['bar'];")
    js("delete x.baz<!JSCODE_ERROR!>()<!>;")
    js("delete <!JSCODE_ERROR!>this<!>;")
}
