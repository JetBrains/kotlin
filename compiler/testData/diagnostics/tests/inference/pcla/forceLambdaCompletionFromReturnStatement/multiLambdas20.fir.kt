// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -PCLAEnhancementsIn21

class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(
    funcA: (Container<B>) -> ((B) -> Unit),
    funcB: (Container<B>) -> Unit,
) {}

fun main() {
    build(
        { container -> { arg -> arg.length } },
        { container -> container.consume("") },
    )
}
