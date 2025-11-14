// RUN_PIPELINE_TILL: FRONTEND

fun guardWithCommaAndIsNegative(x: Any): String =
    when (x) {
        is Int, is Long<!COMMA_IN_WHEN_CONDITION_WITH_WHEN_GUARD!>
        if ((x as Number).toLong() > 0L) < !> -> "positive-number"
        else -> "other"
    }