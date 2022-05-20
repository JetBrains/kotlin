interface Expression<K>

interface ExpressionWithColumnType<K> : Expression<K>

class Column<T>: ExpressionWithColumnType<T>

infix fun <T : Comparable<T>, S : T?> ExpressionWithColumnType<in S>.less(t: T) {}

infix fun <T : Comparable<T>, S : T?> Expression<in S>.less(other: Expression<in S>) {}

fun main(x: Column<Long?>, y: Double) {
    x <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING!>less<!> y // error in 1.7.20, no error in 1.7.0
}
