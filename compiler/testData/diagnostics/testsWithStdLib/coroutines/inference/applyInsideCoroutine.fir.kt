// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// NI_EXPECTED_FILE

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!> {
        yield(4)
    }
}

val test2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(B)
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!> {
        yield(C)
    }
}

val test3 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    this.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>let<!> {
        yield(B)
    }

    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!> {
        yield(C)
    }
}

interface A
object B : A
object C : A
