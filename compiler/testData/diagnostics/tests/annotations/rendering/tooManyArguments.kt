// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
annotation class Ann(val s: String = "")

@Ann("s")
fun foo() {}

val bar = foo(
    <!TOO_MANY_ARGUMENTS("public fun foo(): Unit defined in root package in file tooManyArguments.kt")!>15<!>
)

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, primaryConstructor,
propertyDeclaration, stringLiteral */
