// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend GenericController<S>.(S) -> Unit): S = TODO()

val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>test1<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(<!NI;CONSTANT_EXPECTED_TYPE_MISMATCH!>4<!>)
}

val test2 = generate<Int> {
    yield(4)
}

val test3 = generate { bar: Int ->
    yield(4)
}
