// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-34004

// KT-34004: False-positive USELESS_CAST in local property initializer on smartcasted value

interface A
class B : A

fun test(a: A) {
    if (a !is B) return

    var b = a <!USELESS_CAST!>as B<!>
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration,
isExpression, localProperty, propertyDeclaration, smartcast */
