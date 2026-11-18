// LANGUAGE: +LocalTypeAliases
fun interface IntPredicate {
    fun accept(i: Int): Boolean
}

fun usage() {
    typealias IntPredicateAlias = IntPredicate

    fun check(p: <caret>IntPredicateAlias) {}
}
