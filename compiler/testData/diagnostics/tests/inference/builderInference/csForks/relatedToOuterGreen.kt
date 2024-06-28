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
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>, 1)
        }
    }

    generate {
        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>, 1)
        }

        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>, 1)
        }
    }

    generate {
        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>, <!TYPE_MISMATCH!>w<!>)
        }

        if (a is B) {
            baz(<!DEBUG_INFO_SMARTCAST!>a<!>, <!TYPE_MISMATCH!>w<!>)
        }
    }

    generate {
        if (a is B || a is C) {
            baz(a, w)
        }
    }
}
