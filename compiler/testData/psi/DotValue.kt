enum class Color { BLUE, RED; }

val color: Color = .RED

fun isBlue(color: Color) = color == .BLUE
fun isRed(color: Color) = .RED == color

fun noSemicolon() = with(1) {
    val x = "hello"
    .RED
}

fun withSemicolon() = with(1) {
    val x = "hello" ;
    .RED
}

fun whenExample(color: Color) = when (color) {
    .RED -> 1 ;
    .BLUE -> 2 ;
}

fun whenExampleSubject(color: Color) = when (color) {
    is .RED -> 1
    is .BLUE -> 2
}

fun whenExampleIs(color: Color) = when {
    color is .RED -> 1
    color is .BLUE -> 2
}
