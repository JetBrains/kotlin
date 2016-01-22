enum class Color { RED, GREEN, BLUE }

fun foo(arr: Array<Color>): Color {
    loop@ for (color in arr) {
        <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (color) {
            Color.RED -> return color
            Color.GREEN -> break@loop
            Color.BLUE -> if (arr.size == 1) return color else continue@loop
        }<!>
        // Unreachable
        <!UNREACHABLE_CODE!>return Color.BLUE<!>
    }
    return Color.GREEN
}