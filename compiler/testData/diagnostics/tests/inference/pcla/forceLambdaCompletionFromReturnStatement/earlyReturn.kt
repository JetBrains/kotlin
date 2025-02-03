// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
class Container<A> {
    fun consume(arg: A) {}
}

fun <B> build(func: (Container<B>) -> B) {}

fun main(b: Boolean) {
    build { container ->
        if (b) {
            return@build <!TYPE_MISMATCH!>{ <!CANNOT_INFER_PARAMETER_TYPE!>arg<!> ->
                <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>arg<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>length<!>
            }<!>
        }
        container.consume({ arg: String -> })
    }
}
