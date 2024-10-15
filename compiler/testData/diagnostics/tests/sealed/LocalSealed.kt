// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun foo() {
    <!WRONG_MODIFIER_TARGET!>sealed<!> class My
}
