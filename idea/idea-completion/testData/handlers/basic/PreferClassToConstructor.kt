class Color {
    companion object {
        val RED = Color()
    }
}

fun getColor(): Color? {
    return Color<caret>
}

// INVOCATION_COUNT: 0
// ELEMENT: *
// CHAR: '.'
