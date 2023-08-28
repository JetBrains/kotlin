// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

class Controller<T : Number> {
    suspend fun yield(t: T) {}
}

fun <S : Number> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(<!ARGUMENT_TYPE_MISMATCH!>"foo"<!>)
}
