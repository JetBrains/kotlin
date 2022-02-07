fun test_1(x: Any, y: Any) {
    when {
        x is String -> "x"
        y is Int -> "y"
        else -> "nothing"
    }
}

fun test_2(x: Any) {
    when (x) {
        is Int -> "Int: $x"
        // $subj$ is Int -> ...
        1 -> "One"
        // $subj$ == 1 -> ...
        null -> "null"
        // x == null
        else -> "nothing"
    }
}

fun getAny(): Any? = null

fun test_2() {
    when (val y = getAny()) {
        // y is String
        is Int -> "Int: $y"
        1 -> "One"
        null -> "null"
        else -> "nothing"
    }
}
