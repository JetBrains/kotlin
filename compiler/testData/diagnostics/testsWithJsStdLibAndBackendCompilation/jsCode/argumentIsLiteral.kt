// FIR_DIFFERENCE
// The diagnostic cannot be implemented with the FIR frontend checker because it requires constant evaluation over FIR.
// The diagnostic is implemented as a klib check over IR.

// IGNORE_BACKEND_K1: JS_IR
val a = "1"

fun nonConst(): String = "1"

fun test() {
    val b = "b"

    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>)
    js((<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>b<!>))
    js(("c"))
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>3<!>)
    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT, TYPE_MISMATCH!>3 + 2<!>)
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>1.0f<!>)
    js(<!CONSTANT_EXPECTED_TYPE_MISMATCH, JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>true<!>)
    js("$<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>")
    js("${1}")
    js("$<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>b<!>;")
    js("${<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>b<!>}bb")
    js(<!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!> + <!JSCODE_ARGUMENT_NON_CONST_EXPRESSION!>a<!>)
    js("a" + "a")
    js("ccc")

    js(<!JSCODE_ARGUMENT_SHOULD_BE_CONSTANT!>nonConst()<!>)
}
