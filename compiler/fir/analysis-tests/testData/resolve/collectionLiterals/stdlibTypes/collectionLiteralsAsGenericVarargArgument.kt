// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

fun <T> takeTs(vararg ts: T) { }

fun test() {
    takeTs<String>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>ts = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!><!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeTs<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>ts = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!><!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeTs<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!><!SPREAD_OF_NULLABLE!>*<!><!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["a", "b", "c"]<!><!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, nullableType, stringLiteral,
typeParameter, vararg */
