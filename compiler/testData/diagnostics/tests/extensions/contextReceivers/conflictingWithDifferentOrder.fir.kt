// !LANGUAGE: +ContextReceivers

interface A
interface B

context(A, B)
fun f(): Unit = TODO()

context(B, A)
fun f(): Unit = TODO()

fun test(a: A, b: B) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
        }
    }
}
