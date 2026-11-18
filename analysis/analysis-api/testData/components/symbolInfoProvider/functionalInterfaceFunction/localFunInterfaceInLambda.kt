val usage = {
    fun interface IntPredicate {
        fun accept(i: Int): Boolean
    }

    fun check(p: <caret>IntPredicate) {}
}
