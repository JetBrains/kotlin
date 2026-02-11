// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

fun test() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        add(<!CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    }

    buildList {
        add([42])
    }

    buildList {
        add([42])
        add(["42"])
    }

    buildList {
        add([])
        add(["42"])
    }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral */
