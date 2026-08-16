// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters

annotation class Ann
class A

fun runWithA(block: context(String) () -> Unit) {
}

val t = context(a: A) fun () { a }
val t2 = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@Ann<!> context(a: A) fun () { a }

fun foo() {
    val t = context(a: A) fun () { a }
    val t2 = <!RUNTIME_ANNOTATION_ON_LAMBDA_IS_NOT_RETAINED!>@Ann<!> context(a: A) fun () { a }
    runWithA(context(a: String) fun () { a })
}

/* GENERATED_FIR_TAGS: annotationDeclaration, anonymousFunction, classDeclaration, functionDeclaration, functionalType,
localProperty, propertyDeclaration, typeWithContext */
