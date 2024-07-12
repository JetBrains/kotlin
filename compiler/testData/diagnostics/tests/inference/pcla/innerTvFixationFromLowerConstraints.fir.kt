class Controller<T> {
    fun yield(t: T): Boolean = true
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun <E4> select(x: E4, y: E4) {}
fun <E5> selectL(x: E5, y: E5, l: (E5) -> Unit) {}

fun main(x: Controller<String>) {
    generate {
        // Just adding the constraints
        // Controller<String> <: E4
        // Controller<S> <: E4
        // No fixation is required
        select(x, this)
    }.length

    generate {
        // Adding the constraints
        // Controller<String> <: E5
        // Controller<S> <: E5
        // But to analyze lambda we fix E5 to CST(Controller<String>, Controller<S>) = Controller<CST(String, S)>
        // The question is what is CST(String, S)
        // And current answer in K2 is that it's String just the same way as when while fixating some TV, it has improper lower constraits
        // See org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver.prepareLowerConstraints
        selectL(x, this) { x ->
            x.yield(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
            x.yield("")
        }
    }.length
}