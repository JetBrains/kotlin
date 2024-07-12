// FIR_IDENTICAL

class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <R> myRun(b: () -> R) {}
fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(b: Boolean) {
    generate {
        myRun {
            if (b) {
                yield("")
            }
        }
        Unit
    }
}
