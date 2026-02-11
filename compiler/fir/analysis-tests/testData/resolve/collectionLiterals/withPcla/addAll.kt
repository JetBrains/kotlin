// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun ambiguity() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    }

    buildList {
        addAll([1, 2, 3])
    }

    buildList {
        addAll([42])
        addAll(["42"])
    }
}

fun resolvesToCollection() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        addAll(0, <!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    }

    buildList {
        addAll(0, [1, 2, 3])
    }

    buildList {
        addAll(0, [42])
        addAll(0, ["42"])
    }

    buildSet {
        addAll([])
        addAll([1, 2, 3])
    }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */
