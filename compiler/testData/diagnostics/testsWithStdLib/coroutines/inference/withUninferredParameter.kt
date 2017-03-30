// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend GenericController<S>.(S) -> Unit): S = TODO()

val test1 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(4)
}

val test2 = generate<Int> {
    yield(4)
}

val test3 = generate { bar: Int ->
    yield(4)
}
