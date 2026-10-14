const val constant: Int = 1

const val negative: Int = -2

const val string: String = "s"

const val reference: Int = constant

val nonConst: Int = 3

val inferred = 4

fun expressionBody(): Int = 5

fun inferredExpressionBody() = 6

val getterBody: Int
    get() = 7

fun withDefaults(int: Int = 8, string: String = "s") {}

class WithMembers {
    val member: Int = 9

    fun memberBody(): Int = 10
}
