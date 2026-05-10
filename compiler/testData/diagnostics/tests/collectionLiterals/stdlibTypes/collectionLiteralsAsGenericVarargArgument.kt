// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

fun <T> takeTs(vararg ts: T) { }

fun test() {
    takeTs<String>(ts = [])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeTs<!>(ts = <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    takeTs(*["a", "b", "c"])
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, nullableType, stringLiteral,
typeParameter, vararg */
