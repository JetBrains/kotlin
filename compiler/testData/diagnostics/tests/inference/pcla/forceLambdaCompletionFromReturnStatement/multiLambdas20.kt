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
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ container -> { <!CANNOT_INFER_PARAMETER_TYPE!>arg<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>arg<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>length<!> } }<!>,
        <!BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION!>{ container -> container.consume("") }<!>,
    )
}
