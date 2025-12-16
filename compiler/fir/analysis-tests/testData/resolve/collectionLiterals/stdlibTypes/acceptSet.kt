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
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!CANNOT_INFER_PARAMETER_TYPE, INAPPLICABLE_CANDIDATE!>[42, "42"]<!>)

    acceptString([])
    acceptString(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
    acceptString(<!ARGUMENT_TYPE_MISMATCH!>[42, "42"]<!>)
    acceptString(["1", "2", "3"])

    acceptListString([])
    acceptListString([listOf()])
    acceptListString([listOf<String>()])
    acceptListString(<!ARGUMENT_TYPE_MISMATCH!>[listOf<Int>()]<!>)
    acceptListString(<!ARGUMENT_TYPE_MISMATCH!>[listOf(42)]<!>)
    acceptListString([listOf("42")])
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, intersectionType, nullableType, stringLiteral, typeParameter */
