// RUN_PIPELINE_TILL: FRONTEND

fun whenWithInRange(x: Int): String =
    when (x) {
        in 0..9 -> "digit"
        in 10..99 -> "two-digits"
        !in Int.MIN_VALUE..99 -> "big"
        else -> "other"
    }