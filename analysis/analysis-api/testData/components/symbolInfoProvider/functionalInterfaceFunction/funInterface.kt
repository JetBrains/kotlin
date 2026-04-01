fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

fun usage(p: <caret>IntPredicate) {}
