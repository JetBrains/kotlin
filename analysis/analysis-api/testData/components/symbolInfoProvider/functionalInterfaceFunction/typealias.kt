fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

typealias IntPredicateAlias = IntPredicate

fun usage(p: <caret>IntPredicateAlias) {}
