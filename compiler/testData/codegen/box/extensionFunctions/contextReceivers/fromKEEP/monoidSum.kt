// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME

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

context(Monoid<T>)
fun <T> List<T>.sum(): T = fold(unit) { acc, e -> acc.combine(e) }

fun box(): String {
    with(IntMonoid) {
        listOf(1, 2, 3).sum()
    }
    return with(StringMonoid) {
        listOf("O", "K").sum()
    }
}