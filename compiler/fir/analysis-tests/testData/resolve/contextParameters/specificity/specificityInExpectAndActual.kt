// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments +MultiPlatformProjects

// MODULE: m1-common
// FILE: common.kt
class C

expect interface A {
    context(c: C)
    fun foo()
}

fun bar1a(a: A) {
    a.<!NO_CONTEXT_ARGUMENT!>foo<!>()
}

context(c: C)
fun bar2a(a: A) {
    a.foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual interface A {
    context(c: C)
    actual fun foo()

    fun foo()
}

fun bar1b(a: A) {
    a.foo()
}

context(c: C)
fun bar2b(a: A) {
    a.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, propertyDeclaration,
propertyDeclarationWithContext */
