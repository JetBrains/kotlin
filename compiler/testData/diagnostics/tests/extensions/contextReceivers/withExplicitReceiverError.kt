// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class A
class B
class C

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.<!NO_CONTEXT_RECEIVER!>f<!>()
    with(A()) {
        b.f()
    }
    with(C()) {
        b.<!NO_CONTEXT_RECEIVER!>f<!>()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext,
lambdaLiteral, localProperty, propertyDeclaration */
