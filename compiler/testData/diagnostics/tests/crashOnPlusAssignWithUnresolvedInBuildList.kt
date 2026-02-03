// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-84061
// WITH_STDLIB

fun foo() = buildList {
    val arr = Array(1) { <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>iCantSpellMutableSetOf<!><Int>() }
    arr<!NO_SET_METHOD!>[0]<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+=<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>0<!>
    add(arr)
}

/* GENERATED_FIR_TAGS: additiveExpression, assignment, functionDeclaration, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration */
