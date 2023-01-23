fun <T : <!FINAL_UPPER_BOUND!>String<!>> g(): T? = null

fun <R> f(block: () -> R?): R? = block()

fun main() {
    <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR!>f<!><Int> { g() /* OK, g() is inferred into {Int & String}? */ }
}
