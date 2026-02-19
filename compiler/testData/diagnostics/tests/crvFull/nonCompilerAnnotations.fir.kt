// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// FIR_DUMP

@file:NonCompilerAnnotation

@Target(AnnotationTarget.FILE, AnnotationTarget.FUNCTION)
annotation class NonCompilerAnnotation

@NonCompilerAnnotation
fun foo(): String = ""

fun main() {
    <!RETURN_VALUE_NOT_USED!>foo<!>()
}

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, functionDeclaration, stringLiteral */
