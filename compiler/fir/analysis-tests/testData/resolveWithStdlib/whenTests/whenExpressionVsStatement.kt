// RUN_PIPELINE_TILL: FRONTEND

fun whenExpressionVsStatement(x: Int): String {
    var result = ""
    when (x) {
        0 -> result = "zero"
        1 -> result = "one"
        else -> result = "other"
    }

    val asExpression = when (x) {
        0 -> "ZERO"
        else -> "NON_ZERO"
    }

    return result + "-" + asExpression
}