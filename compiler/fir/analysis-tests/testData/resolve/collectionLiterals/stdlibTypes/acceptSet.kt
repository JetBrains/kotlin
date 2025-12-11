// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82638
// LANGUAGE: +CollectionLiterals

fun <T> acceptGeneric(set: Set<T>) {
}

fun acceptString(set: Set<String>) {
}

fun acceptListString(set: Set<List<String>>) {
}

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42, "42"]<!>)

    acceptString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    acceptString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    acceptString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42, "42"]<!>)
    acceptString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["1", "2", "3"]<!>)

    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[<!CANNOT_INFER_PARAMETER_TYPE!>listOf<!>()]<!>)
    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[listOf<String>()]<!>)
    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[listOf<Int>()]<!>)
    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[listOf(42)]<!>)
    acceptListString(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[listOf("42")]<!>)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, nullableType, stringLiteral, typeParameter */
