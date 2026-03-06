// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

fun test() {
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1, 2, 3]<!>.toString()
    <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[]<!><!NO_GET_METHOD!>[0]<!>
    val x = <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[0]<!> + <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[1]<!> + <!UNSUPPORTED_COLLECTION_LITERAL_TYPE!>[2]<!>
}

/* GENERATED_FIR_TAGS: additiveExpression, functionDeclaration, integerLiteral, localProperty, propertyDeclaration */
