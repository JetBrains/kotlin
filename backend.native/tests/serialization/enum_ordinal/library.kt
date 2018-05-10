enum class Color {
    RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW
}

fun determineColor(code: Int): Color = when (code) {
    0 -> Color.BLUE
    1 -> Color.MAGENTA
    else -> Color.CYAN
}