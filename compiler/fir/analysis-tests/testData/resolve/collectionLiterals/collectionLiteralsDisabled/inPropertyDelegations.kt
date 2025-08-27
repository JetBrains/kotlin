// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: -CollectionLiterals

fun test() {
    val x <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> <!UNSUPPORTED!>[1, 2, 3]<!>
    val y: String <!DELEGATE_SPECIAL_FUNCTION_MISSING!>by<!> <!UNSUPPORTED!>["1", "2", "3"]<!>
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, localProperty, nullableType,
propertyDeclaration, propertyDelegate, starProjection, stringLiteral */
