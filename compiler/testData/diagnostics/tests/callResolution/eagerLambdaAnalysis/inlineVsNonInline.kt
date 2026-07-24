// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound

inline fun inlineStringOrInlineUnit(block: () -> String): Int = 1
inline fun inlineStringOrInlineUnit(block: () -> Unit): String = "(2)"

inline fun inlineStringOrUnit(block: () -> String): Int = 1
fun inlineStringOrUnit(block: () -> Unit): String = "(2)"

fun stringOrInlineUnit(block: () -> String): Int = 1
inline fun stringOrInlineUnit(block: () -> Unit): String = "(2)"

fun testInlineStringOrInlineUnit(a: Boolean): Any {
    val unitResult = inlineStringOrInlineUnit {
        if (a) return Unit
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val stringResult = inlineStringOrInlineUnit {
        if (a) return ""
        ""
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    return 1
}

fun testInlineStringOrUnit(a: Boolean): Any {
    val unitResult = inlineStringOrUnit {
        if (a) <!RETURN_NOT_ALLOWED!>return<!> Unit
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val stringResult = inlineStringOrUnit {
        if (a) return ""
        ""
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    return 1
}

fun testStringOrInlineUnit(a: Boolean): Any {
    val unitResult = stringOrInlineUnit {
        if (a) return Unit
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val stringResult = stringOrInlineUnit {
        if (a) <!RETURN_NOT_ALLOWED!>return<!> ""
        ""
    }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>stringResult<!>

    return 1
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, ifExpression, inline, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */
