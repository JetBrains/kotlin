// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

class Controller<T : Number> {
    suspend fun yield(t: T) {}
}

fun <S : Number> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test = <!CANNOT_INFER_PARAMETER_TYPE!>generate<!> {
    yield(<!ARGUMENT_TYPE_MISMATCH!>"foo"<!>)
}
