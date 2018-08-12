// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 1: When expression without bound value (the form where the expression enclosed in parantheses is absent) evaluates one of the many different expressions based on corresponding conditions present in the same when entry.
 NUMBER: 7
 DESCRIPTION: 'When' with if expressions in the control structure body.
 */

enum class Direction {
    NORTH, SOUTH, WEST, EAST
}

sealed class Expr
data class _SealedChild1(val number: Int) : Expr()
data class _SealedChild2(val e1: Int, val e2: Int) : Expr()
data class _SealedChild3(val m1: Int, val m2: Int) : Expr()

fun case_1(value: Int, value1: Int, value2: Boolean?, value3: Direction?, value4: Expr?) {
    when {
        value == 1 -> if (value1 > 1000) "1"
            else if (value1 > 100) "2"
            else if (value1 > 10 || value1 < -10) "3"
            else "4"
        value == 2 -> if (!value2!!) "1"
            else if (<!DEBUG_INFO_SMARTCAST!>value2<!>) "2"
        value == 3 -> if (value2 == null) "1"
            else if (value2 == true) "2"
            else if (value2 == false) "3"
        value == 4 -> if (value3!! == Direction.WEST) "1"
            else if (value3 == Direction.SOUTH) "2"
            else if (value3 == Direction.NORTH) "3"
            else if (value3 == Direction.EAST) "4"
        value == 5 -> if (value3 == null) "1"
            else if (value3 == Direction.WEST) "2"
            else if (value3 == Direction.SOUTH) "3"
            else if (value3 == Direction.NORTH) "4"
            else if (value3 == Direction.EAST) "5"
        value == 6 -> if (value4 is _SealedChild1) "1"
            else if (value4 is _SealedChild2) "2"
            else if (value4 is _SealedChild3) "3"
        value == 7 -> {
            if (value4 == null) {
                "1"
            } else if (value4 is _SealedChild1) {
                "2"
            } else if (value4 is _SealedChild2) {
                "3"
            } else if (value4 is _SealedChild3) {
                "4"
            }
        }
    }
}