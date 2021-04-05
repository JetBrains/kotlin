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
        yield(<!ARGUMENT_TYPE_MISMATCH!>4<!>)
    }
}

val test2 = generate {
    yield(<!ARGUMENT_TYPE_MISMATCH!>B<!>)
    apply {
        yield(<!ARGUMENT_TYPE_MISMATCH!>C<!>)
    }
}

val test3 = generate {
    this.let {
        yield(<!ARGUMENT_TYPE_MISMATCH!>B<!>)
    }

    apply {
        yield(<!ARGUMENT_TYPE_MISMATCH!>C<!>)
    }
}

interface A
object B : A
object C : A
