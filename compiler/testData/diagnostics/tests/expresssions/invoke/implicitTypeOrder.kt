// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT

class A {
    fun bar() = <!OPERATOR_MODIFIER_REQUIRED!>foo<!>() // should resolve to invoke

    fun invoke() = this
}

fun create() = A()

val foo = create()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, thisExpression */
