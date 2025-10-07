// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

fun <T> foo(s: String) = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!CANNOT_INFER_PARAMETER_TYPE!>foo<!>("")<!>

fun <<!CYCLIC_GENERIC_UPPER_BOUND!>T : T<!>> foo(d: Double) {}

fun main() {
    <!NONE_APPLICABLE!>foo<!>(1)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeConstraint, typeParameter */
