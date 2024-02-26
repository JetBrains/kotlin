// !LANGUAGE: +ContextReceivers

interface A
interface B

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

fun test(a: A, b: B) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) {
        <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
        }
    }
}
