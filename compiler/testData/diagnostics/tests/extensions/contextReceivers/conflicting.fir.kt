// !LANGUAGE: +ContextReceivers

interface A
interface B

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

<!CONFLICTING_OVERLOADS!>context(A, B)
fun f(): Unit<!> = TODO()

fun test(a: A, b: B) {
    <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(a) <!CANNOT_INFER_PARAMETER_TYPE!>{
        <!CANNOT_INFER_PARAMETER_TYPE, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>with<!>(b) <!CANNOT_INFER_PARAMETER_TYPE!>{
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>()
        }<!>
    }<!>
}
