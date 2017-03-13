enum class Color(val rgb : Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

enum class Direction {
    NORTH,
    SOUTH,
    WEST,
    EAST
}

fun bar(c: Color) = when (c) {
    Color.RED -> 1
    Color.GREEN -> 2
    Color.BLUE -> 3
}

fun foo(d: Direction) = when(d) {
    Direction.NORTH -> 1
    Direction.SOUTH -> 2
    Direction.WEST -> 3
    Direction.EAST -> 4
}

fun box() : String =
    if (foo(Direction.EAST) == 4 && bar(Color.GREEN) == 2) "OK" else "fail"
