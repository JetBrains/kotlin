// !LANGUAGE: +ContextReceivers

interface A
interface B

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

<!CONFLICTING_OVERLOADS!>context(B, A)
fun f(): Unit<!> = TODO()

fun test(a: A, b: B) {
    <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) {
        with(b) {
            <!ARGUMENT_TYPE_MISMATCH!><!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()<!>
        }
    }
}
