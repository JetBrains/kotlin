// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When valueession
 PARAGRAPH: 6
 SENTENCE 7: The bound valueession is of a sealed class type and all its possible subtypes are covered using type test conditions of this valueession;
 NUMBER: 1
 DESCRIPTION: Check when exhaustive when possible subtypes of the sealed class are covered.
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

sealed class ExprEmpty

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes covered).
fun case_1(value: Expr1): String = when (value) {
    is Const1 -> <!DEBUG_INFO_SMARTCAST!>value<!>.n
    is Sum1 -> <!DEBUG_INFO_SMARTCAST!>value<!>.e1 + <!DEBUG_INFO_SMARTCAST!>value<!>.e2
    is Mul1 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m1 + <!DEBUG_INFO_SMARTCAST!>value<!>.m2
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (single sealed class subtypes covered).
fun case_2(value: Expr1): String = when (value) {
    <!USELESS_IS_CHECK!>is Expr1<!> -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all sealed class subtypes with methods covered).
fun case_3(value: Expr2): String = when (value) {
    is Const2 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m1()
    is Sum2 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m2()
    is Mul2 -> <!DEBUG_INFO_SMARTCAST!>value<!>.m3()
}

// CASE DESCRIPTION: Checking for exhaustive 'when' (all objects covered using implicit equality operator).
fun case_4(value: Expr3): String = when (value) {
    Const1O -> ""
    Sum1O -> ""
    Mul1O -> ""
}

// CASE DESCRIPTION: Checking for exhaustive 'when' on the empty sealed class (without subtypes).
fun case_5(value: ExprEmpty): String = when (value) {
    else -> ""
}
