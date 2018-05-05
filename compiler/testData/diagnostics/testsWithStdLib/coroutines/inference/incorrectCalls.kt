// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class GenericController<T> {
    suspend fun yield(t: T) {}
    fun notYield(t: T) {}

    suspend fun yieldBarReturnType(t: T) = t
    fun barReturnType(): T = TODO()
}

fun <S> generate(g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

val test1 = generate {
    yield(3)
}

val test2 = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(3)
    notYield(3)
}

val test3 = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(3)
    yieldBarReturnType(3)
}

val test4 = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(3)
    barReturnType()
}