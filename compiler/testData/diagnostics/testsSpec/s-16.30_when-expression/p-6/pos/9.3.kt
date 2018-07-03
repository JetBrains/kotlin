// SKIP_TXT

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 9: The bound expression is of a nullable type and one of the cases above is met for its non-nullable counterpart and, in addition, there is a condition containing literal null.
 NUMBER: 3
 DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered and contains a null check.
 */

sealed class Expr1
data class Const1(val n: String) : Expr1()
data class Sum1(val e1: String, val e2: String) : Expr1()
data class Mul1(val m1: String, val m2: String) : Expr1()

sealed class Expr2
class Const2() : Expr2() {
    fun m1(): String {
        return ""
    }
}
class Sum2() : Expr2() {
    fun m2(): String {
        return ""
    }
}
class Mul2() : Expr2() {
    fun m3(): String {
        return ""
    }
}

sealed class Expr3
object Const1O : Expr3()
object Sum1O : Expr3()
object Mul1O : Expr3()

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes and null value covered).
fun case_1(expr: Expr1?): String = when (expr) {
    is Const1 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.n
    is Sum1 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.e1 + <!DEBUG_INFO_SMARTCAST!>expr<!>.e2
    is Mul1 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.m1 + <!DEBUG_INFO_SMARTCAST!>expr<!>.m2
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (sealed class itself and null value covered).
fun case_2(expr: Expr1?): String = when (expr) {
    is Expr1 -> ""
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class with methods subtypes and null value covered).
fun case_3(expr: Expr2?): String = when (expr) {
    is Const2 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.m1()
    is Sum2 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.m2()
    is Mul2 -> <!DEBUG_INFO_SMARTCAST!>expr<!>.m3()
    null -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all objects covered using implicit equality operator and null value covered).
fun case_4(expr: Expr3?): String = when (expr) {
    Const1O -> ""
    Sum1O -> ""
    Mul1O -> ""
    null -> ""
}
