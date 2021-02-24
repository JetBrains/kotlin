// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE: Arithmetic and comparison operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36855
 */
package testPackCase1

class Arithmetic() {
    operator fun invoke(i: Int) = B()
}
class B() {
    operator fun plus(i: Int)  : B =B()
    val plusAssign = Arithmetic()

    fun case7(){
        var b =B()
        <!ASSIGN_OPERATOR_AMBIGUITY!>b+=1<!> //ASSIGN_OPERATOR_AMBIGUITY
        this += 1 //ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR
    }
}

fun case1() {
    var b = B()
    <!ASSIGN_OPERATOR_AMBIGUITY!>b += 1<!> //ASSIGN_OPERATOR_AMBIGUITY
}

// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE: Arithmetic and comparison operators
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-36855
 */
package testPackCase2

class Arithmetic() {
    operator fun invoke(i: Int) = B()
}

class B() {
    operator fun plusAssign(i: Int) {}
    val plus = Arithmetic()


    fun case2(b: B) {
        b += 1 //ok

        var b1 = B()
        <!ASSIGN_OPERATOR_AMBIGUITY!>b1 += 1<!> //ASSIGN_OPERATOR_AMBIGUITY

        this += 1  //ok
    }
}

fun case2() {
    var b = B()
    <!ASSIGN_OPERATOR_AMBIGUITY!>b += 1<!> //ASSIGN_OPERATOR_AMBIGUITY
}
