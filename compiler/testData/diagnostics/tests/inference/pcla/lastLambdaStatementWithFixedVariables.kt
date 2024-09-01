// FIR_IDENTICAL
class Controller<T> {
    fun yield(t: T): Boolean = true
}
fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun <E, F> myMap(x: Controller<F>, y: E, b: (E) -> F) {}

fun <G> myTake(t: G, predicate: (G) -> Unit): G = TODO()

fun foo() {
    generate {
        myMap(this, "") {
            // NB: The statement below was relevant some time ago, but just it doesn't seem so, but left it for history
            // Here, to start analysis of the lambda, we need to fix the G variable and after that,
            // when we add a subtyping constraint from the whole `myTake` call to the last statement of the `myMap` lambda,
            // the return type of the former call contains fixed variable G.
            // That is quite irregular for non-PCLA cases, because ther we don't fix TVs that belong to return types.
            myTake("") { x ->
                x.length
            }
        }
    }
}