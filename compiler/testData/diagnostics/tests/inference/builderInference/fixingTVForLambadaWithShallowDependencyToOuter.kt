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
            this.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>a<!>
            this.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>b<!>
        }

        yield(aI)
    }

    x.a
    x.<!UNRESOLVED_REFERENCE!>b<!>
}