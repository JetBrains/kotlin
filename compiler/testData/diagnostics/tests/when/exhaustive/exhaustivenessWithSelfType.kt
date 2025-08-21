// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78097
// WITH_STDLIB
// LANGUAGE: +DataFlowBasedExhaustiveness

interface MyInterface {
    object A : MyInterface
    object B : MyInterface
    object C : MyInterface
}

fun negSimpleSealed(x: MyInterface): Int {
    if (x !is MyInterface.C) return 0
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyInterface.C -> 1
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration,
isExpression, nestedClass, objectDeclaration, smartcast, whenExpression, whenWithSubject */
