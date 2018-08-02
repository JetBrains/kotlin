// !CHECK_TYPE

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 5
 SENTENCE 1: The type of the resulting expression is the least upper bound of the types of all the entries.
 NUMBER: 4
 DESCRIPTION: 'When' least upper bound of the types check (when exhaustive via sealed class).
 */

sealed class Expr
data class Const(val number: Int) : Expr()
data class Sum(val e1: Int, val e2: Int) : Expr()
data class Mul(val m1: Int, val m2: Int) : Expr()

sealed class Expr2
object ConstO : Expr2()
object SumO : Expr2()
object MulO : Expr2()

open class A {}
open class B: A() {}
open class C: B() {}
open class D: C() {}
class E: D() {}

// CASE DESCRIPTION: Checking correct type in 'when'.
fun case_1(value: Expr): String {
    val whenValue = when (value) {
        is Const -> B()
        is Sum -> C()
        is Mul -> D()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' with null-check branch.
fun case_2(value: Expr?): String {
    val whenValue = when (value) {
        is Const -> B()
        is Sum -> C()
        is Mul -> D()
        null -> E()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when'.
fun case_3(value: Expr): String {
    val whenValue = when (value) {
        is Const -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is Sum -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is Mul -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch.
fun case_4(value: Expr?): String {
    val whenValue = when (value) {
        is Const -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        is Sum -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        is Mul -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' (equality with objects).
fun case_5(value: Expr2): String {
    val whenValue = when (value) {
        ConstO -> B()
        SumO -> C()
        MulO -> D()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct type in 'when' (equality with objects) with null-check branch.
fun case_6(value: Expr2?): String {
    val whenValue = when (value) {
        ConstO -> B()
        SumO -> C()
        MulO -> D()
        null -> E()
    }

    whenValue checkType { _<B>() }
    checkSubtype<A>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' (equality with objects).
fun case_7(value: Expr2): String {
    val whenValue = when (value) {
        ConstO -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        SumO -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        MulO -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking Any type (implicit cast to any) in 'when' with null-check branch (equality with objects).
fun case_8(value: Expr2?): String {
    val whenValue = when (value) {
        ConstO -> <!IMPLICIT_CAST_TO_ANY!>10<!>
        SumO -> <!IMPLICIT_CAST_TO_ANY!>""<!>
        MulO -> <!IMPLICIT_CAST_TO_ANY!>object<!> {}
        null -> {<!IMPLICIT_CAST_TO_ANY!>{}<!>}
    }

    whenValue checkType { _<Any>() }
    checkSubtype<Any>(whenValue)

    return ""
}

// CASE DESCRIPTION: Checking correct basic type (Int) in 'when' with.
fun case_9(value: Expr2): String {
    val whenValue = when (value) {
        <!USELESS_IS_CHECK!>is Expr2<!> -> 10
    }

    whenValue checkType { _<Int>() }
    checkSubtype<Int>(whenValue)

    return ""
}