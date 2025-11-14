// RUN_PIPELINE_TILL: FRONTEND

fun whenWithNotIs(x: Any?): String =
    when (x) {
        !is String -> "not-string"
        else -> x.length.toString()
    }