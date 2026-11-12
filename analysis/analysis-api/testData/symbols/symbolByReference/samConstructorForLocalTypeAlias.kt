// LANGUAGE: +LocalTypeAliases

package test

fun usage() {
    fun interface IntPredicate {
        fun accept(i: Int): Boolean
    }

    typealias IntPredicateAlias = IntPredicate

    val isEven = IntPredicate<caret>Alias { it % 2 == 0 }
}
