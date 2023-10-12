// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// ERROR_POLICY: SEMANTIC
// !DIAGNOSTICS: -UNUSED_PARAMETER
fun Int.foo(x: Int) {
    js("this = x<!JSCODE_ERROR!><!>;")
}
