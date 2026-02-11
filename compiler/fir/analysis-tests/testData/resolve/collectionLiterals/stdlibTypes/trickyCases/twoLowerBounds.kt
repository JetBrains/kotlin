// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun <K> select(vararg k: K): K = k[0]

fun test() {
    select(mutableSetOf<String>(), mutableSetOf<Int>(), [])
    select(setOf<String>(), setOf<Int>(), [])
    select(setOf<String>(), setOf<Int>(), [42])

    // ambiguity
    select(setOf<String>(), mutableSetOf<String>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>)

    // ambiguity
    <!CANNOT_INFER_PARAMETER_TYPE!>select<!>(<!CANNOT_INFER_PARAMETER_TYPE!>setOf<!>(), <!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>(), <!AMBIGUOUS_COLLECTION_LITERAL!>[42]<!>)
}

/* GENERATED_FIR_TAGS: capturedType, collectionLiteral, functionDeclaration, integerLiteral, intersectionType,
nullableType, outProjection, typeParameter, vararg */
