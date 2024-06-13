// DIAGNOSTICS: +UNUSED_EXPRESSION

fun unusedExpressions() {
    if (1 == 1)
        fun(): Int {return 1}
    else
        fun() = 1

    if (1 == 1) {
        fun(): Int {
            return 1
        }
    }
    else
        fun() = 1

    when (1) {
        0 -> fun(): Int {return 1}
        else -> fun() = 1
    }

    fun() = 1
}
