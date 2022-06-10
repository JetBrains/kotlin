// !DIAGNOSTICS: -UNUSED_PARAMETER

class Controller<T : Number> {
    suspend fun yield(t: T) {}
}

fun <S : Number> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test = <!NEW_INFERENCE_ERROR!>generate {
    yield("foo")
}<!>
