// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test1 = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    apply {
        yield(4)
    }
}

val test2 = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(B)
    apply {
        yield(C)
    }
}

val test3 = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    this.<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>let<!> {
        yield(B)
    }

    apply {
        yield(C)
    }
}

interface A
object B : A
object C : A