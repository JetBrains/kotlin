// IGNORE_BACKEND_FIR: JVM_IR
enum class Color { RED, GREEN, BLUE }

fun foo(arr: Array<Color>): Color {
    loop@ for (color in arr) {
        when (color) {
            Color.RED -> return color
            Color.GREEN -> break@loop
            Color.BLUE -> if (arr.size == 1) return color else continue@loop
        }
    }
    return Color.GREEN
}

fun box() = if (foo(arrayOf(Color.BLUE, Color.GREEN)) == Color.GREEN) "OK" else "FAIL"