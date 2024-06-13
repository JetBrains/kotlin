// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// IGNORE_BACKEND_K1: JS_IR
// DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Any) {
    js("delete x.foo;")
    js("delete x['bar'];")
    js(<!JSCODE_ERROR!>"delete x.baz();"<!>)
    js(<!JSCODE_ERROR!>"delete this;"<!>)
}
