enum class Direction {
    NORTH,
    SOUTH(val x : Int) {
        fun again() : String {
            return "Hello"
        }

        class Hello
    },
    WEST {
        class Some {
            fun test() : Int {
                return 12 + 14
            }
        }
    }
    EAST
}