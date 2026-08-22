// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
annotation class Ann(vararg val i: String)

const val topLevel = "topLevel"

fun foo() {
    val a1 = "a"
    val a2 = "b"
    val a3 = a1 + a2

    val a4 = 1
    val a5 = 1.0

    @Ann(
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!>,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a2<!>,
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a3<!>,
            "$topLevel",
            "$<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!>",
            "$<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!> $topLevel",
            "$<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a4<!>",
            "$<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a5<!>",
            <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a1<!> + a2,
            "a" + <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a2<!>,
            "a" + topLevel,
            "a" + <!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>a4<!>
    ) val b = 1
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, const, functionDeclaration, integerLiteral,
localProperty, outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
