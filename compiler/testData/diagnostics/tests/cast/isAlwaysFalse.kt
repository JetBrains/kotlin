abstract class A
abstract class B : A()

abstract class AS
abstract class BS : AS()

interface AI
interface BI : AI

fun test_1(a: A) {
    <!USELESS_IS_CHECK("true")!>a is A<!> // always true
    a is B

    <!USELESS_IS_CHECK("false")!>a is AS<!> // always false
    <!USELESS_IS_CHECK("false")!>a is BS<!> // always false

    a is AI
    a is BI
}

fun test_2(a: A) {
    <!USELESS_IS_CHECK("false")!>a !is A<!> // always false
    a !is B

    <!USELESS_IS_CHECK("true")!>a !is AS<!> // always true
    <!USELESS_IS_CHECK("true")!>a !is BS<!> // always true

    a !is AI
    a !is BI
}

fun test_3(a: Any) {
    if (a is A) {
        <!USELESS_IS_CHECK("true")!>a is A<!> // always true
        a is B

        <!USELESS_IS_CHECK("false")!>a is AS<!> // always false
        <!USELESS_IS_CHECK("false")!>a is BS<!> // always false

        a is AI
        a is BI
    }
}

fun test_4(a: A) {
    when (a) {
        <!USELESS_IS_CHECK("true")!>is A<!> -> {} // always true
        is B -> {}

        <!USELESS_IS_CHECK("false")!>is AS<!> -> {} // always false
        <!USELESS_IS_CHECK("false")!>is BS<!> -> {} // always false

        is AI -> {}
        is BI -> {}
    }
}

fun test_5(a: A) {
    when (a) {
        <!USELESS_IS_CHECK("false")!>!is A<!> -> {} // always false
        !is B -> {}

        <!USELESS_IS_CHECK("true")!>!is AS<!> -> {} // always true
        !is BS -> {} // here a may has type AS (by data flow)

        !is AI -> {}
        !is BI -> {}
    }
}
