// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

object Rem {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun mod(x: Int) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun modAssign(x: Int) {}
}
