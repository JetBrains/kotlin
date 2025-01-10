// RUN_PIPELINE_TILL: FRONTEND
class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<F>.baz(a: A<F>, f: F) {}

fun <T> bar(a: A<T>, w: T) {
    generate {
        if (a is B) {
            baz(a, 1)
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>w<!>)
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }
    }

    generate {
        baz(a, w)

        if (a is B) {
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>1<!>)
            baz(a, w)
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }
    }

    generate {
        if (a is B) {
            baz(a, 1)
        }

        if (a is B) {
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>w<!>)
        }
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
