interface Intf {
    fun run()
}

abstract class Ordinary

annotation class Anno

@Anno
class Generic<T> : Ordinary(), Intf {
    override fun run() {}
}

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

sealed class Operation {
    class Add(val firstValue: Int, val secondValue: Int) : Operation()
    class Subtract(val minuend: Int, val subtrahend: Int) : Operation()
    class Negate(val value: Int) : Operation()
}