// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases

fun foo1(): Int = <!REDUNDANT_RETURN!>return<!> 42
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo2<!>() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_AND_IMPLICIT_TYPE!>return<!> <!RETURN_TYPE_MISMATCH!>42<!>
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo3<!>() = run { <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING!>return<!> <!RETURN_TYPE_MISMATCH!>42<!> }
fun foo4(): Int = run { return 42 }
fun foo5(b: Boolean) = when (1) {
    else -> {
        val foo = if (b) <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY_WARNING!>return<!> "" else ""
        foo
    }
}

fun foo6(b: Boolean): String = when (1) {
    else -> {
        val foo = if (b) return "" else ""
        foo
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
