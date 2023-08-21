class Controller<T> {
    fun yield(t: T): Boolean = true
    fun get(): T = TODO()
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun <R> foo(w: R, x: (R) -> R): R = TODO()

fun <Q> materialize(): Q = TODO()

fun bar(x: String) {}

fun main() {
    generate {
        // S <: String
        bar(get())
        // R <: S
        // R <: String
        // R = String
        yield(foo(materialize()) { it.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>toString<!>() })
    }

    generate {
        // String <: S
        yield("")
        // S <: R
        // => String <: R
        // R = CST(S, String) = String
        foo(<!TYPE_MISMATCH, TYPE_MISMATCH!>get()<!>) { it.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, DEBUG_INFO_UNRESOLVED_WITH_TARGET, UNRESOLVED_REFERENCE!>length<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>toString<!>() }
    }
}