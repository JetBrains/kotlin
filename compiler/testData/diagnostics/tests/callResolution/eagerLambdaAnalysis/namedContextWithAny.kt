// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +InferThrowableTypeParameterToUpperBound, +ContextParameters, +ExplicitContextArguments

context(a: () -> Any)
fun foo() = 1

context(a: () -> Unit)
<!CONTEXTUAL_OVERLOAD_SHADOWED!>fun foo()<!> = "2"

fun test() {
    with({ 2 }) {
        val result = foo()
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }

     with<()->Unit, ()-> Unit>({ Unit }) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
     }

    val result2 = foo(a = { "" })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result2<!>

    val result3 = foo(a = { Unit })
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result3<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration, stringLiteral */
