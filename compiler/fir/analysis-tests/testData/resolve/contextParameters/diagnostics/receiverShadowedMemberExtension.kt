// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// RENDER_DIAGNOSTICS_FULL_TEXT

class Foo {
    fun Bar.baz() {}
}

class Bar

fun Foo.dispatchReceiverShadowed() {
    context(Foo()) {
        Bar().<!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>baz<!>()
    }
}

fun Foo.extensionReceiverShadowed() {
    with(Bar()) {
        context(Bar()) {
            <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>baz<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, lambdaLiteral */
