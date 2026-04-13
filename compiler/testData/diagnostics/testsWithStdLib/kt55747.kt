// RUN_PIPELINE_TILL: FRONTEND

object Rem {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun mod(x: Int) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun modAssign(x: Int) {}
}

/* GENERATED_FIR_TAGS: functionDeclaration, objectDeclaration, operator */
