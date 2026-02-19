// RUN_PIPELINE_TILL: FRONTEND
// SKIP_KT_DUMP

fun test() {
    for (i in 0..0) fun <!SINGLE_ANONYMOUS_FUNCTION_WITH_NAME_ERROR!>x<!>() {}
}

/* GENERATED_FIR_TAGS: forLoop, functionDeclaration, integerLiteral, localFunction, localProperty, propertyDeclaration,
rangeExpression */
