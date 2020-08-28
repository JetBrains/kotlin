// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test1 = generate {
    apply {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(4)
    }
}

val test2 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>(B)
    apply {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(C)
    }
}

val test3 = generate {
    this.let {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(B)
    }

    apply {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(C)
    }
}

interface A
object B : A
object C : A