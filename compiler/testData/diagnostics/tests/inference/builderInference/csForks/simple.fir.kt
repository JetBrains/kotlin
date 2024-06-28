class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<*>.baz(a: A<F>, f: F) {}

fun <T> bar(a: A<T>, w: T) {
    <!INFERENCE_UNSUCCESSFUL_FORK, INFERENCE_UNSUCCESSFUL_FORK!>generate<!> {
        yield("")
        baz(a, w)

        if (a is B) {
            baz(a, 1)
            baz(a, w)
            <!INFERENCE_UNSUCCESSFUL_FORK!>baz<!>(a, "")
        }

        <!INFERENCE_UNSUCCESSFUL_FORK!>if (a is B || a is C) {
            <!INFERENCE_UNSUCCESSFUL_FORK!>baz<!>(a, w)
        }<!>
    }
}
