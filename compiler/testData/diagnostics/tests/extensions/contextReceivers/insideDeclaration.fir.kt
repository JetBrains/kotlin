// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class A {
    fun h1() {}
}

class B {
    fun h2() {}
}

fun B.foo() {
    <!UNRESOLVED_REFERENCE!>h1<!>()
    h2()
}

context(A)
fun B.bar() {
    <!UNRESOLVED_REFERENCE!>h1<!>()
    h2()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext */
