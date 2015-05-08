// "Change to short enum entry super constructor in the whole project" "true"

enum class First(val colorCode: Int) {
    RED: First(0xff0000),
    GREEN: First(0x00ff00)<caret>, BLUE: First(0x0000ff)
}

enum class Second(val dirCode: Int) {
    NORTH: Second(1) {
        override fun dir(): String = "N"
    },
    SOUTH: Second(2) {
        override fun dir(): String = "S"
    },
    WEST : Second(3) {
        override fun dir(): String = "W"
    },
    EAST:  Second(4) {
        override fun dir(): String = "E"
    };


    abstract fun dir(): String
}
