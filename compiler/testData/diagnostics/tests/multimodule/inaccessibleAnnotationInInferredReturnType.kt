// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-80247
// MODULE: a
// FILE: a.kt
@Target(AnnotationTarget.TYPE)
annotation class Anno

// MODULE: b(a)
// FILE: b.kt
fun f(): @Anno String = ""

// MODULE: c(b)
// FILE: c.kt
fun g() = f()

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, stringLiteral */
