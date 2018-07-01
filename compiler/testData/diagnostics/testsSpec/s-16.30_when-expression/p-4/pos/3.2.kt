// !DIAGNOSTICS: -UNUSED_EXPRESSION

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 3: Type test condition: type checking operator followed by type.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and type test condition (with sealed class).
 */

sealed class Expr
data class Const(val number: Int) : Expr()
data class Sum(val e1: Int, val e2: Int) : Expr()
data class Mul(val m1: Int, val m2: Int) : Expr()

sealed class ExprEmpty

// CASE DESCRIPTION: 'When' with type test condition on the all possible subtypes of the sealed class.
fun case_1(value: Expr): String = when (value) {
    is Const -> ""
    is Sum -> ""
    is Mul -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the not all possible subtypes of the sealed class.
fun case_2(value: Expr): String {
    <!NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS!>when<!> (value) {
        is Const -> return ""
        is Sum -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with type test condition on the not all possible subtypes of the sealed class and 'else' branch.
fun case_3(value: Expr): String = when (value) {
    is Const -> ""
    is Sum -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the all possible subtypes of the sealed class and 'else' branch (redundant).
fun case_4(value: Expr): String = when (value) {
    is Const -> ""
    is Sum -> ""
    is Mul -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the empty sealed class.
fun case_5(value: ExprEmpty): String = when (value) {
    else -> ""
}
