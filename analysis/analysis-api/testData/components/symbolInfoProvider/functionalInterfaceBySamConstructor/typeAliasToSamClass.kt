fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

typealias IntPredicateAlias = IntPredicate

val isEven = IntPredicateAlias { it % 2 =<caret>= 0 }
