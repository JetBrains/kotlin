enum class Color { BLUE, RED; }

val color: Color = _.RED

fun isBlue(color: Color) = color == _.BLUE
fun isRed(color: Color) = _.RED == color

fun noSemicolon() = with(1) {
    val x = "hello"
    _.RED
}

fun withSemicolon() = with(1) {
    val x = "hello" ;
    _.RED
}

fun whenExample(color: Color) = when (color) {
    _.RED -> 1
    _.BLUE -> 2
}

fun whenExampleSubject(color: Color) = when (color) {
    is _.RED -> 1
    is _.BLUE -> 2
}

fun whenExampleIs(color: Color) = when {
    color is _.RED -> 1
    color is _.BLUE -> 2
}
