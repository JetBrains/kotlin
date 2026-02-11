// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

class Param
class C {
    val c = 42
}
class R {
    val r = 42
}

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

context(C)
fun R.f1(g: context(C) R.(Param) -> Unit) {
    g(this<!UNRESOLVED_LABEL!>@C<!>, this<!UNRESOLVED_LABEL!>@R<!>, Param())
}

context(C)
fun R.f2(g: @MyAnnotation context(C) R.(Param) -> Unit) {
    g(this<!UNRESOLVED_LABEL!>@C<!>, this<!UNRESOLVED_LABEL!>@R<!>, Param())
}

context(C)
fun f3(g: context(C) (Param) -> Unit) {
    g(this<!UNRESOLVED_LABEL!>@C<!>, Param())
}

context(C)
fun R.f4(g: context(C) R.() -> Unit) {
    g(this<!UNRESOLVED_LABEL!>@C<!>, this<!UNRESOLVED_LABEL!>@R<!>)
}

context(C)
fun f5(g: context(C) () -> Unit) {
    g(this<!UNRESOLVED_LABEL!>@C<!>)
}

context(C)
fun f6(g: (context(C) () -> Unit)?) {
    g?.invoke(this<!UNRESOLVED_LABEL!>@C<!>)
}

fun test() {
    val lf1: context(C) R.(Param) -> Unit = { _ ->
        r
        <!UNRESOLVED_REFERENCE!>c<!>
    }
    val lf2: @MyAnnotation context(C) R.(Param) -> Unit = { _ ->
        r
        <!UNRESOLVED_REFERENCE!>c<!>
    }
    val lf3: context(C) (Param) -> Unit = { _ ->
        <!UNRESOLVED_REFERENCE!>c<!>
    }
    val lf4: context(C) R.() -> Unit = {
        r
        <!UNRESOLVED_REFERENCE!>c<!>
    }
    val lf5: context(C) () -> Unit = {
        <!UNRESOLVED_REFERENCE!>c<!>
    }
    val lf6: (context(C) () -> Unit)? = {
        <!UNRESOLVED_REFERENCE!>c<!>
    }

    with(C()) {
        with(R()) {
            f1(lf1)
            f1 { _ ->
                r
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }

            f2(lf2)
            f2 { _ ->
                r
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }

            f3(lf3)
            f3 { _ ->
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }

            f4(lf4)
            f4 {
                r
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }

            f5(lf5)
            f5 {
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }

            f6(lf6)
            f6 {
                <!RECEIVER_SHADOWED_BY_CONTEXT_PARAMETER!>c<!>
            }
        }
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionDeclarationWithContext, functionalType, integerLiteral, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, safeCall, thisExpression, typeWithContext, typeWithExtension */
