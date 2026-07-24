// LANGUAGE: +CollectionLiterals
// RUN_PIPELINE_TILL: FRONTEND

fun cond(): Boolean = true

fun <K> throughTwoLambdas(k: K, vararg l: (K) -> Unit) { }

fun <K: MutableSet<Int>> throughDeclared(k: K, l: (K) -> Unit) { }

fun test() {
    throughTwoLambdas(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>, { _: Set<Int> -> }, { _: List<Int> -> })
    throughTwoLambdas([], { _: Set<Int> -> }, { _: MutableSet<Int> -> })
    throughTwoLambdas([], { _: Set<CharSequence> -> }, { _: MutableSet<String> -> })
    throughTwoLambdas(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>, { _: Set<Int> -> }, { _: MutableSet<String> -> })
    throughTwoLambdas(<!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>[]<!>, { _: Set<Int> -> }, { _: Set<String> -> })
    throughTwoLambdas([], { _: Set<Int> -> }, { _: Set<Int> -> })
    throughTwoLambdas(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>, { _: MutableSet<Int> -> }, { _: MutableSet<String> -> })

    throughDeclared(<!AMBIGUOUS_COLLECTION_LITERAL!>[]<!>) { _: List<Int> -> }
    throughDeclared([]) { _: MutableSet<Int> -> }
    throughDeclared(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>) { _: MutableSet<String> -> }
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, functionalType, intersectionType, lambdaLiteral,
nullableType, typeConstraint, typeParameter, vararg */
