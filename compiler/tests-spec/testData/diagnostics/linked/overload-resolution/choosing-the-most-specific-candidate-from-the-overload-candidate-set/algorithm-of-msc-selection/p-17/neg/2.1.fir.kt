// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1
class A : I1, I2

interface I1
operator fun I1.invoke(): String = TODO()
interface I2
operator fun I2.invoke(): String = TODO()

fun case1(a: A) {
    a.<!AMBIGUITY!>invoke<!>()
    <!AMBIGUITY!>a<!>()
    <!AMBIGUITY!>A()()<!>
}

// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 */
package testPackCase2
interface I : Interface1, Interface2

interface Interface1
operator fun Interface1.invoke(): String = TODO()
interface Interface2
operator fun Interface2.invoke(): String = TODO()

fun case1(a: I) {
    a.<!AMBIGUITY!>invoke<!>()
    <!AMBIGUITY!>a<!>()

    val x  = <!AMBIGUITY!>object : I {} ()<!>
}

// FILE: TestCase3.kt
/*
 * TESTCASE NUMBER: 3
 */
package testPackCase3

interface I : Interface1, Interface2

interface Interface1
operator fun Interface1.invoke(x :() -> Unit): String = TODO()
interface Interface2
operator fun Interface2.invoke(x :() -> Unit): String = TODO()

fun case1(a: I) {
    a.<!AMBIGUITY!>invoke<!>{}
    <!AMBIGUITY!>a<!>{}

    val x  = <!AMBIGUITY!>object : I {} {}<!>
}
