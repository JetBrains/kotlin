// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81841

fun <T> foo(b: (Any, T) -> Unit) { }
fun of(vararg args: Any) {}

fun main() {
    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(::<!INAPPLICABLE_CANDIDATE!>of<!>) // Works in K1, though
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, nullableType, typeParameter, vararg */
