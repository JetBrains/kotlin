fun interface Predicate<A> {
   fun accept(value: A): Boolean
}

val isEven: Predicate<Int> = Predicate { it % 2 =<caret>= 0 }
