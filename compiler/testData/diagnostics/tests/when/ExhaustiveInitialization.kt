enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    val res: Int
    // See KT-6046: res is always initialized
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (dir) {
        Direction.NORTH -> res = 1
        Direction.SOUTH -> res = 2
        Direction.WEST  -> res = 3
        Direction.EAST  -> res = 4
    }<!>
    return res
}