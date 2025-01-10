// RUN_PIPELINE_TILL: FRONTEND
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
            baz(a, <!ARGUMENT_TYPE_MISMATCH!>""<!>)
        }

        if (a is B || a is C) {
            baz(a, w)
        }
    }
}
