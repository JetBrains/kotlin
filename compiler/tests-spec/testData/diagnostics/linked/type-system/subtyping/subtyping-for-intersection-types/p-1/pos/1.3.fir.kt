// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-464
 * MAIN LINK: type-system, subtyping, subtyping-for-intersection-types -> paragraph 1 -> sentence 1
 * PRIMARY LINKS: type-system, subtyping, subtyping-for-intersection-types -> paragraph 2 -> sentence 3
 * NUMBER: 3
 * DESCRIPTION: intersection type inferred for function
 * HELPERS: checkType, functions
 */

// TESTCASE NUMBER: 1
// UNEXPECTED BEHAVIOUR
// ISSUES: KT-40074
fun case1(x: Any) {
    if (x is A1 && x is B1) {
        checkSubtype<A1>(x)
        checkSubtype<B1>(x)

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any & A1 & B1")!>x<!>  //   A1 & B1 & kotlin.Any
    }
}

interface A1
interface B1

// TESTCASE NUMBER: 2
fun <T> case2(x: T) {
    if (x is B2 && x is A2) {
        checkSubtype<A2>(x)
        checkSubtype<B2>(x)
        checkSubtype<T>(x)

        x //NI A2 & B2 & T & T!! OI A2 & B2 & T
        <!DEBUG_INFO_EXPRESSION_TYPE("T & B2 & A2 & T!!")!>x<!>
    }
}

interface A2
interface B2

// TESTCASE NUMBER: 3
fun <T> case3a(x: T) {
    if (x is A3 && x is B3) {

        checkSubtype<A3>(x)
        checkSubtype<B3>(x)
        checkSubtype<T>(x)
        <!DEBUG_INFO_EXPRESSION_TYPE("T & A3 & B3 & T!!")!>x<!>
    }
}
fun <T: Any> case3b(x: T) {
    if (x is A3 && x is B3) {

        checkSubtype<A3>(x)
        checkSubtype<B3>(x)
        checkSubtype<T>(x)

        <!DEBUG_INFO_EXPRESSION_TYPE("T & A3 & B3 & T")!>x<!>
    }
}
fun <T: Any?> case3c(x: T) {
    if (x is A3 && x is B3) {

        checkSubtype<A3>(x)
        checkSubtype<B3>(x)
        checkSubtype<T>(x)

        <!DEBUG_INFO_EXPRESSION_TYPE("T & A3 & B3 & T!!")!>x<!>
    }
}

interface B3
interface A3

// TESTCASE NUMBER: 4

fun case4(x: C?) {
    if (x is B4 && <!USELESS_IS_CHECK!>x is A4<!>) {
        x
        <!DEBUG_INFO_EXPRESSION_TYPE("C? & C")!>x<!>
        x.foo()
    }
}

interface A4
interface B4

class C : A4, B4 {
    fun foo() = ""
}

// TESTCASE NUMBER: 5

fun <T : I5> case5(x: T) {
    if (x is B5 && x is A5) {
        x
        <!DEBUG_INFO_EXPRESSION_TYPE("T & B5 & A5 & T")!>x<!>
    }
}

interface A5 : I5
interface B5 : I5
interface I5
class C5 : A5, B5 {
    fun foo() = ""
}

// TESTCASE NUMBER: 6

fun <T> case6(case: T): T where T : A6?, T : B6?  = case

fun <T> foo(case: T): T? = case

fun <T>test6(x: T) where T : B6? {
    if (x is A6?) {
        (x as C6).foo()
    }
}

interface A6
interface B6

fun C6?.foo(vararg x: Int) = println("hehe")

class C6 : A6, B6 {}
