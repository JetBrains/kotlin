// RUN_PIPELINE_TILL: FRONTEND

fun leaf(a: Int) {}

fun mid(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>leaf<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

fun top(<!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>...<!DEBUG_INFO_MISSING_UNRESOLVED!>mid<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>$props<!><!>) {}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration,
stringLiteral */
