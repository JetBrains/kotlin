// LANGUAGE: +LocalTypeAliases
fun usage() {
    fun interface IntPredicate {
        fun accept(i: Int): Boolean
    }

    typealias IntPredicateAlias = IntPredicate

    fun check(p: <caret>IntPredicateAlias) {}
}
