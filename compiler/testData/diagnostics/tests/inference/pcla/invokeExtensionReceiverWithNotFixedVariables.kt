// FIR_IDENTICAL

interface Controller<F> {
    fun yield(t: F)
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun foo(
    prop: Controller<String>.() -> Unit
) {
    generate {
        prop()
    }
}