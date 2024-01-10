enum class Direction {
    NORTH {
        override val code: Int
            get() = 1
    },

    EAST {
        override val code: Int
            get() = <expr>2</expr>
    },

    SOUTH {
        override val code: Int
            get() = 3
    },

    WEST {
        override val code: Int
            get() = 4
    };

    abstract val code: Int
}