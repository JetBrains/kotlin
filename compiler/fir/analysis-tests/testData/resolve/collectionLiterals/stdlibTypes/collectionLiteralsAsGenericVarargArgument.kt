// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81722
// LANGUAGE: +CollectionLiterals

@file:OptIn(ExperimentalCollectionLiterals::class)

fun <T> takeTs(vararg ts: T) { }

fun test() {
    takeTs<String>(ts = [])
    <!CANNOT_INFER_PARAMETER_TYPE!>takeTs<!>(ts = <!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeTs<!>(*<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>["a", "b", "c"]<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, nullableType, stringLiteral,
typeParameter, vararg */
