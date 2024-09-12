class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<*>.baz(a: A<F>, f: F) {}

fun <T> bar(a: A<T>, w: T) {
    generate {
        yield("")
        baz(a, w)

        if (a is B) {
            baz(a, 1)
            baz(a, w)
            baz(<!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>a<!>, "")
        }

        if (a is B || a is C) {
            <!ARGUMENT_TYPE_MISMATCH!><!CANNOT_INFER_PARAMETER_TYPE!>baz<!>(<!ARGUMENT_TYPE_MISMATCH!>a<!>, <!ARGUMENT_TYPE_MISMATCH!>w<!>)<!>
        }
    }
}
