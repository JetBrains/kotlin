// "Cast expression '40 + 2' to 'Direction'" "true"
enum class Direction {
    NORTH
}

fun foo(d: Direction) {
    when(d) {
        Direction.NORTH -> 0
        40 + 2<caret> -> 1
    }
}
