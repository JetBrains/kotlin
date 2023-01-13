enum class Direction {
    NORTH, EAST, SOUTH, WEST
}

fun usage() {
    <!DEBUG_INFO_CALLABLE_OWNER("Direction.values in Direction")!>Direction.values()<!>
}