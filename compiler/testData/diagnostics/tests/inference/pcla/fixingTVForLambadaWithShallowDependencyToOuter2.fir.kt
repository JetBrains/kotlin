class Controller<T> {
    fun yield(t: T): Boolean = true
    fun get(): T = TODO()
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun bar(x: Any) {}
fun <R> myWith(r: R, b: (R) -> Unit) {}

fun main() {
    generate {
        // S <: Any
        bar(get())

        // S <: R
        // Any <: R
        myWith(get()) {
            it.<!UNRESOLVED_REFERENCE!>length<!>
        }

        yield("")
    }.length
}