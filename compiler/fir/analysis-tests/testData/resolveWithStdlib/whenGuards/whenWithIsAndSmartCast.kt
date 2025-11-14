// RUN_PIPELINE_TILL: FRONTEND

fun whenWithIsAndSmartCast(x: Any): Int =
    when (x) {
        is Int -> x + 1
        is String -> x.length
        else -> -1
    }