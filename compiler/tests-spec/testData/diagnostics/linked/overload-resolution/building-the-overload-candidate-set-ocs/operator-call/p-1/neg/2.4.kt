// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 4
 * DESCRIPTION: without inline plusAssign functions
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39819
 */


// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 * NOTE:
 */
package testPackCase1
import LibPackCase1.a.*
import LibPackCase1.b.*

fun case1 (){
    var b: B? = B()
    <!DEBUG_INFO_CALL("fqName: testPackCase1.B.plus; typeCall: operator function"), INVISIBLE_MEMBER, TYPE_MISMATCH!>b <!UNSAFE_OPERATOR_CALL!>+=<!> { C() }<!>

    <!DEBUG_INFO_CALL("fqName: testPackCase1.B.plus; typeCall: operator function"), INVISIBLE_MEMBER, TYPE_MISMATCH!>b <!UNSAFE_OPERATOR_CALL!>+=<!> {<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>}<!>
}

class B {
    private  operator fun plusAssign(c: () -> C) {}
    private  operator fun plus(c: () -> C): C = c() //(3)
}
class C

// FILE: LibCase1b.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B
import testPackCase1.C

operator fun B?.plusAssign( c: ()->C) {} //(2)


// FILE: LibCase1a.kt
package LibPackCase1.a
import testPackCase1.B

operator fun B?.plusAssign( c: ()->Int) {} //(1)

// FILE: TestCase2.kt
/*
 * TESTCASE NUMBER: 2
 * NOTE:
 */
package testPackCase2
import LibPackCase2.a.*
import LibPackCase2.b.*

fun case2 (){
    var b: B = B()
    <!DEBUG_INFO_CALL("fqName: testPackCase2.B.plus; typeCall: operator function"), INVISIBLE_MEMBER, TYPE_MISMATCH!>b += { C() }<!>

    <!DEBUG_INFO_CALL("fqName: testPackCase2.B.plus; typeCall: operator function"), INVISIBLE_MEMBER, TYPE_MISMATCH!>b += {<!CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH, CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>}<!>
}

class B {
    private  operator fun plusAssign(c: () -> C) {}
    private  operator fun plus(c: () -> C): C = c() //(3)
}
class C

// FILE: LibCase2b.kt
package LibPackCase2.b
import LibPackCase2.a.*
import testPackCase2.B
import testPackCase2.C

operator fun B.plusAssign( c: ()->C) {} //(2)


// FILE: LibCase2a.kt
package LibPackCase2.a
import testPackCase2.B

operator fun B.plusAssign( c: ()->Int) {} //(1)