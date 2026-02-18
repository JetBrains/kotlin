// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun ambiguity() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }
}

fun resolvesToCollection() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(0, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(0, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(0, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[42]<!>)
        addAll(0, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["42"]<!>)
    }

    <!CANNOT_INFER_PARAMETER_TYPE!>buildSet<!> {
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>)
        addAll(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>)
    }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */
