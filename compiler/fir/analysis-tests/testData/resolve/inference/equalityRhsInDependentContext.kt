// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81763
// LANGUAGE: +ResolveEqualsRhsInDependentContextWithCompletion

fun foo() {
    var x: Int? = 1

    if (0.hashCode() == 0) {
        x = null
    }

    if (x == run { x = 10; null }) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int?")!>x<!>
    }
}

fun bar() {
    var x: Int? = 1

    if (0.hashCode() == 0) {
        x = null
    }

    if (x == run { x = 20; null } && x != null) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
    }
}

fun baz() {
    var x: Int? = null

    if (x == run { x = 30; null } && x != null) {
        val y = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>x<!>
    }
}

/* GENERATED_FIR_TAGS: andExpression, assignment, equalityExpression, functionDeclaration, ifExpression, integerLiteral,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast */
