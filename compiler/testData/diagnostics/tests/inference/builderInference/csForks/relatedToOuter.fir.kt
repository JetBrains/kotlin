class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<F>.baz(a: A<F>, f: F) {}

fun <T> bar(a: A<T>, w: T) {
    <!INFERENCE_UNSUCCESSFUL_FORK, INFERENCE_UNSUCCESSFUL_FORK, INFERENCE_UNSUCCESSFUL_FORK!>generate<!> {
        <!INFERENCE_UNSUCCESSFUL_FORK!>if (a is B) {
            baz(a, 1)
            <!INFERENCE_UNSUCCESSFUL_FORK!>baz<!>(a, w)
            <!INFERENCE_UNSUCCESSFUL_FORK!>baz<!>(a, "")
        }<!>
    }

    generate {
        baz(a, w)

        if (a is B) {
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>1<!>)
            baz(a, w)
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }
    }

    <!INFERENCE_UNSUCCESSFUL_FORK, INFERENCE_UNSUCCESSFUL_FORK!>generate<!> {
        if (a is B) {
            baz(a, 1)
        }

        <!INFERENCE_UNSUCCESSFUL_FORK!>if (a is B) {
            <!INFERENCE_UNSUCCESSFUL_FORK!>baz<!>(a, w)
        }<!>
    }

    generate {
        baz(a, w)

        if (a is B) {
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>1<!>)
        }

        if (a is B) {
            baz(a, w)
        }
    }

    generate {
        if (a is B || a is C) {
            baz(a, w)
        }
    }
}
