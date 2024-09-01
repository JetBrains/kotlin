// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

interface A
interface B

context(A, B)
<!CONFLICTING_OVERLOADS!>fun f(): Unit<!> = TODO()

context(A, B)
<!CONFLICTING_OVERLOADS!>fun f(): Unit<!> = TODO()

fun test(a: A, b: B) {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) {
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
        }
    }
}
