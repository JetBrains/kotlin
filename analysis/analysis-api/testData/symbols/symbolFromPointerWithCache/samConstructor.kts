fun interface IntPredicate {
   fun accept(i: Int): Boolean
}

val isEven = Int<caret>Predicate { it % 2 == 0 }