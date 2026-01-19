// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
        add(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */
