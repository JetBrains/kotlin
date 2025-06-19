// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78097
// WITH_STDLIB

interface MySealedInterface {
    object A : MySealedInterface
    object B : MySealedInterface
    object C : MySealedInterface
}

fun negSimpleSealed(x: MySealedInterface): Int {
    if (x !is MySealedInterface.C) return 0
    return when (x) {
        MySealedInterface.C -> 1
    }
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, interfaceDeclaration,
isExpression, nestedClass, objectDeclaration, smartcast, whenExpression, whenWithSubject */
