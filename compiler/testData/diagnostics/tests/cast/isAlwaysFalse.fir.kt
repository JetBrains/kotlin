// RUN_PIPELINE_TILL: FRONTEND
abstract class A
abstract class B : A()

abstract class AS
abstract class BS : AS()

interface AI
interface BI : AI

fun test_1(a: A) {
    <!USELESS_IS_CHECK!>a is A<!> // always true
    a is B

    <!IMPOSSIBLE_IS_CHECK_ERROR!>a is AS<!> // always false
    <!IMPOSSIBLE_IS_CHECK_ERROR!>a is BS<!> // always false

    a is AI
    a is BI
}

fun test_2(a: A) {
    <!USELESS_IS_CHECK!>a !is A<!> // always false
    a !is B

    <!IMPOSSIBLE_IS_CHECK_ERROR!>a !is AS<!> // always true
    <!IMPOSSIBLE_IS_CHECK_ERROR!>a !is BS<!> // always true

    a !is AI
    a !is BI
}

fun test_3(a: Any) {
    if (a is A) {
        <!USELESS_IS_CHECK!>a is A<!> // always true
        a is B

        <!IMPOSSIBLE_IS_CHECK_ERROR!>a is AS<!> // always false
        <!IMPOSSIBLE_IS_CHECK_ERROR!>a is BS<!> // always false

        a is AI
        a is BI
    }
}

fun test_4(a: A) {
    when (a) {
        <!USELESS_IS_CHECK!>is A<!> -> {} // always true
        is B -> {}

        <!IMPOSSIBLE_IS_CHECK_ERROR!>is AS<!> -> {} // always false
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is BS<!> -> {} // always false

        is AI -> {}
        is BI -> {}
    }
}

fun test_5(a: A) {
    when (a) {
        <!USELESS_IS_CHECK!>!is A<!> -> {} // always false
        !is B -> {}

        <!IMPOSSIBLE_IS_CHECK_ERROR!>!is AS<!> -> {} // always true
        !is BS -> {} // here a may has type AS (by data flow)

        !is AI -> {}
        !is BI -> {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, smartcast, whenExpression, whenWithSubject */
