// !LANGUAGE: +ContextReceivers

interface Semigroup<T> {
    infix fun T.combine(other: T): T
}

interface Monoid<T> : Semigroup<T> {
    val unit: T
}
object IntMonoid : Monoid<Int> {
    override fun Int.combine(other: Int): Int = this + other
    override val unit: Int = 0
}
object StringMonoid : Monoid<String> {
    override fun String.combine(other: String): String = this + other
    override val unit: String = ""
}

public inline fun <T, R> Iterable<T>.fold(initial: R, operation: (acc: R, T) -> R): R = TODO()

context(Monoid<T>)
fun <T> List<T>.sum(): T = fold(unit) { acc, e -> acc.combine(e) }

fun <T> listOf(vararg items: T): List<T> = null!!

fun test() {
    with(IntMonoid) {
        listOf(1, 2, 3).sum()
    }
    with(StringMonoid) {
        listOf(1, 2, 3).<!NO_CONTEXT_RECEIVER!>sum()<!>
        listOf("1", "2", "3").sum()
    }
}