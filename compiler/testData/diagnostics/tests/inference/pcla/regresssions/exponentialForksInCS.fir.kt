class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun Controller<*>.foo() {}
fun <V, Y> V.bar(y: Y) {}

interface A<E>

interface B : A<Int>
interface C : A<Long>

fun <F> Controller<F>.baz(a: A<F>, f: F) {}

fun <T> bar(a: A<T>, w: T) {
    generate {
        if (a is B) {
            baz(a, 1)
        }

        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
        foo()
    }
}
