// !LANGUAGE: -ForbidInferringTypeVariablesIntoEmptyIntersection
fun <T : <!FINAL_UPPER_BOUND!>String<!>> g(): T? = null

fun <R> f(block: () -> R?): R? = block()

fun main() {
    f<Int> { <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>g<!>() /* OK, g() is inferred into {Int & String}? */ }
}
