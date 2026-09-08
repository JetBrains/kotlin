// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

fun foo() {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildMap<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>getOrPut<!>(42) { <!CANNOT_INFER_PARAMETER_TYPE!>[]<!> }.<!UNRESOLVED_REFERENCE!>add<!>("42")
    }

    buildMap<_, MutableList<String>> {
        getOrPut(42) { [] }.add("42")
    }

    buildMap<_, Set<String>> {
        getOrPut(42) { [] }.<!UNRESOLVED_REFERENCE!>add<!>("42")
    }

    buildMap<_, MutableSet<String>> {
        getOrPut(42) { [] }.add("42")
    }

    buildMap<_, MutableSet<*>> {
        getOrPut(42) { ["42"] }.addAll([])
    }

    val emptyMutableSet: MutableSet<String> = []

    buildMap {
        this[0] = emptyMutableSet
        getOrPut(42) { [] }.add("42")
    }

    buildMap {
        this[42] = ["42"]
    }
}

/* GENERATED_FIR_TAGS: assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, starProjection, stringLiteral, thisExpression */
