// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals

class Unrelated

fun test() {
    <!UNRESOLVED_REFERENCE!>[]<!>
    val x: Any = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>
    val y: Unrelated = <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>

    for (lst in <!ITERATOR_MISSING, UNRESOLVED_REFERENCE!>[<!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>, <!UNRESOLVED_REFERENCE!>['a', 'b', 'c']<!>, <!UNRESOLVED_REFERENCE!>["a", "b", "c"]<!>]<!>) {
        <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>.toString()
        <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>::toString
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, forLoop, functionDeclaration, integerLiteral, localProperty,
propertyDeclaration, stringLiteral */
