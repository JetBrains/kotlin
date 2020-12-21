// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Controller<T : Number> {
    suspend fun yield(t: T) {}
}

fun <S : Number> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>generate<!> {
    yield("foo")
}
