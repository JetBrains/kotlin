// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

interface A
interface B

context(A, B)
fun f(): Unit = TODO()

context(B, A)
fun f(): Unit = TODO()

fun test(a: A, b: B) {
    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(a) {
        <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(b) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, interfaceDeclaration, lambdaLiteral */
