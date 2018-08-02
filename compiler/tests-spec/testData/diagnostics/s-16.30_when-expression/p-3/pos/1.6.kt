// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 6
 DESCRIPTION: 'When' with exhaustive when expression in the control structure body.
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

sealed class Expr
data class Const(val number: Int) : Expr()
data class Sum(val e1: Int, val e2: Int) : Expr()
data class Mul(val m1: Int, val m2: Int) : Expr()

fun case_1(value: Int, value1: Int, value2: Boolean?, value3: Direction?, value4: Expr?) {
    when {
        value == 1 -> when {
            value1 > 1000 -> "1"
            value1 > 100 -> "2"
            value1 > 10 || value1 < -10 -> "3"
            else -> "4"
        }
        value == 2 -> when(value2!!) {
            true -> "1"
            false -> "2"
        }
        value == 3 -> when(value2) {
            true -> "1"
            false -> "2"
            null -> "3"
        }
        value == 4 -> when(value3!!) {
            Direction.WEST -> "1"
            Direction.SOUTH -> "2"
            Direction.NORTH -> "3"
            Direction.EAST -> "4"
        }
        value == 5 -> when(value3) {
            Direction.WEST -> "1"
            Direction.SOUTH -> "2"
            Direction.NORTH -> "3"
            Direction.EAST -> "4"
            null -> "5"
        }
        value == 6 -> when(value4!!) {
            is Const -> "1"
            is Sum -> "2"
            is Mul -> "3"
        }
        value == 7 -> {
            when(value4) {
                is Const -> "1"
                is Sum -> "2"
                is Mul -> "3"
                null -> "4"
            }
        }
    }
}