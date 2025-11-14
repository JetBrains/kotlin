// RUN_PIPELINE_TILL: FRONTEND

fun whenWithExpressionSubject(a: Int, b: Int): String =
    when (a + b) {
        0 -> "zero"
        in 1..10 -> "small"
        else -> "big"
    }