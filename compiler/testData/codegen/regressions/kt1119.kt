enum class Direction() {
    NORTH {
        val someSpecialValue = "OK"
    }

    SOUTH
    WEST
    EAST
}

fun box() = Direction.NORTH.someSpecialValue
