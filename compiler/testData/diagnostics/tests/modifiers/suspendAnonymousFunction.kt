// FIR_IDENTICAL
// ISSUE: KT-57991

fun foo() {
    <!WRONG_MODIFIER_TARGET!>suspend<!> fun() {

    }
}
