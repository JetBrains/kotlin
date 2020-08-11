// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 1.4-rfc+0.3-603
 * MAIN LINK: declarations, classifier-declaration, data-class-declaration -> paragraph 2 -> sentence 14
 * NUMBER: 1
 * DESCRIPTION: The number of functions is MORE as the number of data propertie
 */

// TESTCASE NUMBER: 1
data class A1( val a: Int,  val b: C1)
class C1

fun case1() {
    val x = A1(1, C1())
    x.component1()
    x.component2()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component3<!>() //nok
}

// TESTCASE NUMBER: 2
data class A2( val a: Int,  val b: String = "s")

fun case2() {
    val x = A2(1)
    x.component1()
    x.component2()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component3<!>() //nok
}

// TESTCASE NUMBER: 3
data class A3( val a: Int = 1,  val b: String = "s")

fun case3() {
    val x = A3()
    x.component1()
    x.component2()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component3<!>() //nok
}


// TESTCASE NUMBER: 4
data class A4( val a: CharSequence)

fun case4() {
    val x = A4("")
    x.component1()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component2<!>() //nok
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component3<!>() //nok
}

// TESTCASE NUMBER: 5
data class A5( val a: CharSequence = "")

fun case5() {
    val x = A5()
    x.component1()
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component2<!>() //nok
    x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>component3<!>() //nok
}