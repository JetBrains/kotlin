// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

suspend fun <S> GenericController<List<S>>.yieldGenerate(g: suspend GenericController<S>.() -> Unit): Unit = TODO()

val test1 = generate {
    // TODO: KT-15185
    <!TYPE_MISMATCH, TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR!>yieldGenerate<!> {
        yield(4)
    }
}