// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER -UNUSED_VARIABLE
// NI_EXPECTED_FILE

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

val test1 = generate {
    apply {
        yield(4)
    }
}

val test2 = generate {
    yield(B)
    apply {
        yield(C)
    }
}

val test3 = generate {
    this.let {
        yield(B)
    }

    apply {
        yield(C)
    }
}

interface A
object B : A
object C : A
