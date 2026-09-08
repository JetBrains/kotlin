// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
annotation class Ann(vararg val i: Boolean)
fun foo() {
    val a1 = 1 > 2
    val a2 = 1 == 2
    val a3 = a1 == a2
    val a4 = a1 > a2

    @Ann(
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!>,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a2<!>,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a3<!>,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!> > a2,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!> == a2
    ) val b = 1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, comparisonExpression, equalityExpression, functionDeclaration,
integerLiteral, localProperty, primaryConstructor, propertyDeclaration, vararg */
