// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <K> id2(x: K, s: String): K = x
fun <K> ret(s: String): K = TODO()

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>id2<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>, "foo")
    <!CANNOT_INFER_PARAMETER_TYPE!>id2<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>, <!ARGUMENT_TYPE_MISMATCH!>42<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>ret<!>("foo")
    <!CANNOT_INFER_PARAMETER_TYPE!>ret<!>(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, nullableType, stringLiteral, typeParameter */
