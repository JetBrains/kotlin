// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84061
// WITH_STDLIB

fun foo() = buildList {
    val arr = Array(1) { iCantSpellMutableSetOf<Int>() }
    arr[0] += 0
    add(arr)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
