// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidImplicitTypeAnnotationWithMissingDependency
// ISSUE: KT-80247
// MODULE: a
// FILE: a.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

// MODULE: b(a)
// FILE: b.kt
fun f(): @Anno String = ""

fun interface Sam {
    fun foo(x: @Anno String)
}

// MODULE: c(b)
// FILE: c.kt
fun g() = <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>f<!>()

val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{}<!>

fun local() {
    val x = f()

    val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_ERROR!>{}<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, stringLiteral */
