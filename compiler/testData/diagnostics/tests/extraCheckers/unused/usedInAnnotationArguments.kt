// RUN_PIPELINE_TILL: FRONTEND
annotation class Ann(val value: Int)

fun foo(): Int {
    val x = 3
    @Ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>x<!>) val y = 5
    return y
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, localProperty, primaryConstructor,
propertyDeclaration */
