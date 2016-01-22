enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (dir) {
        Direction.NORTH -> return 1
        Direction.SOUTH -> throw AssertionError("!!!")
        Direction.WEST  -> return 3
        Direction.EAST  -> return 4
    }<!>
    // Error: Unreachable code. Return is not required.
    <!UNREACHABLE_CODE!>if (dir == Direction.SOUTH) return 2<!>
}