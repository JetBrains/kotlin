// RUN_PIPELINE_TILL: FRONTEND
annotation class Anno(val position: String)

fun foo() {
    class MyClass {
        val prop = 0

        <!WRONG_ANNOTATION_TARGET!>@Anno("init $<!NON_CONST_VAL_USED_IN_CONSTANT_EXPRESSION!>prop<!>")<!>  init {

        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, init, integerLiteral, localClass,
primaryConstructor, propertyDeclaration, stringLiteral */
