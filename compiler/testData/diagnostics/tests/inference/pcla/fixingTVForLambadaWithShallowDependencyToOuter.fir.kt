class Controller<T> {
    fun yield(t: T): Boolean = true
    fun get(): T = TODO()
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

interface A {
    val a: Any
}

interface B : A {
    val b: Any
}

fun <R> myWith(r: R, b: R.() -> Unit) {}

fun main(aI: A, bI: B) {
    val x = generate {
        // B <: S
        yield(bI)

        // S <: R
        // B <: R
        // R = B
        // S = B
        myWith(get()) {
            this.a
            this.b
        }

        yield(<!ARGUMENT_TYPE_MISMATCH!>aI<!>)
    }

    x.a
    x.b
}