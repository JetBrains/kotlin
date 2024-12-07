// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
fun foo() {
    <!WRONG_MODIFIER_TARGET!>sealed<!> class My
}
