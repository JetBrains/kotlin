// FULL_JDK

import java.util.function.*

annotation class Some

enum class Direction {
    @Some NORTH, SOUTH, WEST, EAST
}

enum class Color(val rgb: Int = 5) {
        RED(0xFF0000),
        GREEN(0x00FF00),
        BLUE("0x0000FF");

        constructor(y: String) : this(y.toInt())
}

enum class ProtocolState {
    WAITING {
        override fun signal() = TALKING
    },

    TALKING {
        override fun signal() = WAITING
    };

    abstract fun signal(): ProtocolState
}

enum class IntArithmetics : BinaryOperator<Int>, IntBinaryOperator {
    PLUS {
        override fun apply(t: Int, u: Int): Int = t + u
    },
    TIMES {
        override fun apply(t: Int, u: Int): Int = t * u
    };

    override fun applyAsInt(t: Int, u: Int) = apply(t, u)
}

class C {
    val enumConst: Direction? = Direction.EAST
}
