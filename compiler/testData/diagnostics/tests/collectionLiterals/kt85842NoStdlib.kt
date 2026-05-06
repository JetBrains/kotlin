// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

fun <T> id(t: T): T = t

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!CANNOT_INFER_PARAMETER_TYPE!>id<!> { <!UNRESOLVED_REFERENCE!>[]<!> })
    <!UNRESOLVED_REFERENCE!>[{<!UNRESOLVED_REFERENCE!>[]<!>}]<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, nullableType, typeParameter */
