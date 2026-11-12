// LANGUAGE: +LocalTypeAliases
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION

package test

fun usage() {
    fun interface IntPredicate {
        fun accept(i: Int): Boolean
    }

    typealias IntPredicateAlias = IntPredicate

    val isEven = IntPredicate<caret>Alias { it % 2 == 0 }
}
