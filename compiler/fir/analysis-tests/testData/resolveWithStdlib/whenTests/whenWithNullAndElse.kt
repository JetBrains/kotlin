// RUN_PIPELINE_TILL: FRONTEND

fun whenWithNullAndElse(x: String?): Int =
    when (x) {
        null -> 0
        "" -> 1
        else -> x.length
    }