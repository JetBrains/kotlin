// !LANGUAGE: -ExperimentalBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface Base

interface Controller<T> : Base {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

suspend fun Base.baseExtension() {}

val test1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield("foo")
    baseExtension()
}
