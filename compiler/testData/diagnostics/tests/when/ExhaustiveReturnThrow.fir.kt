enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

fun foo(dir: Direction): Int {
    when (dir) {
        Direction.NORTH -> return 1
        Direction.SOUTH -> throw AssertionError("!!!")
        Direction.WEST  -> return 3
        Direction.EAST  -> return 4
    }
    // Error: Unreachable code. Return is not required.
    if (dir == Direction.SOUTH) return 2
}