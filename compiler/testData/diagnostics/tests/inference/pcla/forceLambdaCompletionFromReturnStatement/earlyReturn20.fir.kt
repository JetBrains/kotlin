// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -PCLAEnhancementsIn21

class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(func: (Container<B>) -> B) {}

fun main(b: Boolean) {
    build { container ->
        if (b) {
            return@build { arg ->
                arg.length
            }
        }
        container.consume({ arg: String -> })
    }
}
