// MODULE: dep
// FILE: predicate.kt
fun interface Predicate<A> {
   fun accept(value: A): Boolean
}

// MODULE: main(dep)
// FILE: main.kt
val isEven: Predicate<Int> = Predicate { it % 2 =<caret>= 0 }
