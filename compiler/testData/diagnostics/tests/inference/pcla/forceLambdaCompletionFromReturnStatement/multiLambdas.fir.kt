// RUN_PIPELINE_TILL: FRONTEND
class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(
    funcA: (Container<B>) -> ((B) -> Unit),
    funcB: (Container<B>) -> Unit,
) {}

fun main() {
    build(
        { container -> { arg -> arg.<!UNRESOLVED_REFERENCE!>length<!> } },
        { container -> container.consume("") },
    )
}
