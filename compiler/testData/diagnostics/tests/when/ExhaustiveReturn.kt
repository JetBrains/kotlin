enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (dir) {
        Direction.NORTH -> return 1
        Direction.SOUTH -> return 2
        Direction.WEST  -> return 3
        Direction.EAST  -> return 4
    }<!>
    // See KT-1882: no return is needed at the end
}