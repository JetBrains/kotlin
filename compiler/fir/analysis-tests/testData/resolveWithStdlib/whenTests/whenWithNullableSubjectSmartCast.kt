// RUN_PIPELINE_TILL: FRONTEND

fun whenWithNullableSubjectSmartCast(x: String?): Int =
    when (x) {
        null -> 0
        else -> x.length
    }