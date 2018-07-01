// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 6
 SENTENCE 7: The bound expression is of a sealed class type and all its possible subtypes are covered using type test conditions of this expression;
 NUMBER: 1
 DESCRIPTION: Checking for not exhaustive when when not covered by all possible subtypes.
 */

sealed class Expr
data class Const(val number: Int) : Expr()
data class Sum(val e1: Int, val e2: Int) : Expr()
data class Mul(val m1: Int, val m2: Int) : Expr()
object A: Expr() {}

sealed class Expr2
data class Const2(val number: Int) : Expr2()

sealed class Expr3
object A2: Expr3() {}

sealed class ExprEmpty

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking and equality with object).
fun case_1(value: Expr): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is Const -> ""
    is Sum -> ""
    A -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class (type checking).
fun case_2(value: Expr): String = <!NO_ELSE_IN_WHEN!>when<!>(value) {
    is Const -> ""
    is Sum -> ""
    is Mul -> ""
}

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with several subtypes (no branches).
fun case_3(value: Expr): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the sealed class with one subtype (no branches).
fun case_4(value: Expr3): Int = <!NO_ELSE_IN_WHEN!>when<!>(value) { }

// CASE DESCRIPTION: Checking for not exhaustive 'when' on the empty sealed class (without subtypes).
fun case_5(value: ExprEmpty): String = <!NO_ELSE_IN_WHEN!>when<!> (value) { }