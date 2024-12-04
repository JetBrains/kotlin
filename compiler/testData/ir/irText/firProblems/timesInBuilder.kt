// WITH_STDLIB

private fun test(x: Long) =
    countIssues {
        +(spentTime lessEq 2 * 60)
        +(spentTime lessEq id(2 * 60))
        +(spentTime.select(2 * 60, x))
    }

val spentTime = integer("spentTime")

fun integer(name: String) = Column()

fun <I> id(arg: I): I = arg

infix fun <T : Comparable<T>> Column.lessEq(t: T) = Expression()
fun <T : Comparable<T>> Column.select(t: T, r: T) = Expression()

class Expression

class Column

class ArgumentsBuilder {
    val arguments = mutableListOf<Expression>()

    operator fun Expression.unaryPlus() {
        arguments.add(this)
    }
}

private fun countIssues(restrictionsBuilder: ArgumentsBuilder.() -> Unit) {}
