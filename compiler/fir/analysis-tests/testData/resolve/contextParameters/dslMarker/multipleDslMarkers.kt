// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

@DslMarker annotation <!DSL_MARKER_WITH_DEFAULT_TARGETS!>class Dsl<!>

@Dsl class A
@Dsl class B

fun withContext(block: context(A, B) () -> Unit){
    return null!!
}
context(a: A, b: B)
fun foo() { }

context(a: A)
fun bar() { }

context(a: A, b: B)
val property: String
    get() = ""

fun usage(){
    withContext {
        <!DSL_SCOPE_VIOLATION!>foo<!>()
        <!DSL_SCOPE_VIOLATION!>bar<!>()
        <!DSL_SCOPE_VIOLATION!>property<!>
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, checkNotNullCall, classDeclaration, functionDeclaration,
functionDeclarationWithContext, functionalType, getter, lambdaLiteral, propertyDeclaration,
propertyDeclarationWithContext, stringLiteral, typeWithContext */
