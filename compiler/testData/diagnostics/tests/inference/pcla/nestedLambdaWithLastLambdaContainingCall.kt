// FIR_IDENTICAL

class Controller<T> {
    fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit) {}

fun main() {
    generate {
        myRun {
            yield("")
            myLet {}
        }
    }
}

fun myLet(x: () -> Unit) {}
fun <E> myRun(x: () -> E) {}