// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidImplicitTypeAnnotationWithMissingDependency
// RENDER_DIAGNOSTICS_FULL_TEXT
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
fun g() = <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING!>f<!>()

val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING!>{}<!>

fun local() {
    val x = f()

    val sam = Sam <!MISSING_DEPENDENCY_IN_INFERRED_TYPE_ANNOTATION_WARNING!>{}<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, stringLiteral */
