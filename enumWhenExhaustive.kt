// RUN_PIPELINE_TILL: FRONTEND

enum class Color { RED, GREEN, BLUE }

fun enumWhenExhaustive(c: Color): Int =
    when (c) {
        Color.RED -> 1
        Color.GREEN -> 2
        Color.BLUE -> 3
    }
