// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun cond(): Boolean = true

fun <K> throughTwoLambdas(k: K, vararg l: (K) -> Unit) { }

fun <K: MutableSet<Int>> throughDeclared(k: K, l: (K) -> Unit) { }

fun test() {
    throughTwoLambdas(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, { _: Set<Int> -> }, { _: List<Int> -> })
    throughTwoLambdas([], { _: Set<Int> -> }, { _: MutableSet<Int> -> })
    throughTwoLambdas(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, { _: Set<Int> -> }, { _: MutableSet<String> -> })
    throughTwoLambdas(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, { _: Set<Int> -> }, { _: Set<String> -> })
    throughTwoLambdas([], { _: Set<Int> -> }, { _: Set<Int> -> })
    throughTwoLambdas(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>, { _: MutableSet<Int> -> }, { _: MutableSet<String> -> })

    throughDeclared(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>) { _: List<Int> -> }
    throughDeclared([]) { _: MutableSet<Int> -> }
    throughDeclared(<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>) { _: MutableSet<String> -> }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, functionalType, intersectionType, lambdaLiteral,
nullableType, typeConstraint, typeParameter, vararg */
