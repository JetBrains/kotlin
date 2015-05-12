sealed class Operation(val left: Int, val right: Int) {
    abstract fun exec(): Int
    class Plus(left: Int, right: Int): Operation(left, right) {
        override fun exec(): Int = left + right
    }
    class Minus(left: Int, right: Int): Operation(left, right) {
        override fun exec(): Int = left - right
    }
    class Times(left: Int, right: Int): Operation(left, right) {
        override fun exec(): Int = left * right
    }
    class Slash(left: Int, right: Int): Operation(left, right) {
        override fun exec(): Int = left / right
    }
}

fun priority(op: Operation) = when(op) {
    is Operation.Plus, is Operation.Minus  -> 1
    is Operation.Times, is Operation.Slash -> 2
}
