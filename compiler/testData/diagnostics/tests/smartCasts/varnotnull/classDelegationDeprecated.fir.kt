// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-57417

interface HasProperty {
    val property: Int?
}

class Test(delegate: HasProperty) : HasProperty by delegate

fun test(a: Test) {
    if (a.property != null) <!DEPRECATED_SMARTCAST_ON_DELEGATED_PROPERTY!>a.property<!> + 1
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, equalityExpression, functionDeclaration, ifExpression,
inheritanceDelegation, integerLiteral, interfaceDeclaration, nullableType, primaryConstructor, propertyDeclaration,
smartcast */
