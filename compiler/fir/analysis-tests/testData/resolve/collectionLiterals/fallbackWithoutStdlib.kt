// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class Unrelated

fun test() {
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!>
    val x: Any = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>
    val y: Unrelated = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>

    for (lst in <!ITERATOR_MISSING, UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[<!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>['a', 'b', 'c']<!>, <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>["a", "b", "c"]<!>]<!>) {
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>.toString()
        <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>::toString
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, forLoop, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
