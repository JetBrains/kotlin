// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -AllowReturnInExpressionBodyWithExplicitType, -ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases

fun foo1(): Int = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> 42
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo2<!>() = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> <!RETURN_TYPE_MISMATCH!>42<!>
fun <!IMPLICIT_NOTHING_RETURN_TYPE!>foo3<!>() = run { return <!RETURN_TYPE_MISMATCH!>42<!> }
fun foo4(): Int = run { return 42 }
fun foo5(b: Boolean) = when (1) {
    else -> {
        val foo = if (b) return "" else ""
        foo
    }
}

fun foo6(b: Boolean): String = when (1) {
    else -> {
        val foo = if (b) return "" else ""
        foo
    }
}

fun fooUnit1(b: Boolean) = run {
    if (b) return
    Unit
}

fun fooUnit2(b: Boolean) = when(1) {
    else -> {
        val foo = if (b) return else ""
        Unit
    }
}

typealias MyUnit = Unit

fun fooUnit3(b: Boolean, myUnit: MyUnit) = when(1) {
    else -> {
        val foo = if (b) return else ""
        myUnit
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, stringLiteral, whenExpression, whenWithSubject */
