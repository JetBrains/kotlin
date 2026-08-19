// RUN_PIPELINE_TILL: FRONTEND
annotation class Ann(vararg val i: Boolean)
fun foo() {
    val bool1 = true

    @Ann(<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>bool1<!>) val a = bool1
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, localProperty, primaryConstructor,
propertyDeclaration, vararg */
