// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-57417
// LANGUAGE: +UnstableSmartcastOnDelegatedProperties

interface HasProperty {
    val property: Int?
}

class Test(delegate: HasProperty) : HasProperty by delegate

fun test(a: Test) {
    if (a.property != null) <!SMARTCAST_IMPOSSIBLE!>a.property<!> + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
inheritanceDelegation, integerLiteral, interfaceDeclaration, nullableType, primaryConstructor, propertyDeclaration,
smartcast */
