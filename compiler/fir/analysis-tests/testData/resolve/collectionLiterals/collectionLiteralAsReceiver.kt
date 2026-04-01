// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

fun test() {
    <!UNRESOLVED_REFERENCE!>[1, 2, 3]<!>.toString()
    <!UNRESOLVED_REFERENCE!>[]<!><!NO_GET_METHOD!>[0]<!>
    val x = <!UNRESOLVED_REFERENCE!>[0]<!> + <!UNRESOLVED_REFERENCE!>[1]<!> + <!UNRESOLVED_REFERENCE!>[2]<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
