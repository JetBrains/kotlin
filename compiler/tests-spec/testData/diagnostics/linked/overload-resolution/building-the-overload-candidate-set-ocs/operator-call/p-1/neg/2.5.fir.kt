// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 5
 * DESCRIPTION: inline plusAssign functions
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-39819
 */

// TESTCASE NUMBER: 1
// FILE: TestCase1.kt
package testPackCase1
import LibPackCase1.a.*
import LibPackCase1.b.*
fun case1 (){
    var b: B? = null
    <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!>b <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> { C() }<!>

    <!DEBUG_INFO_CALL("fqName: fqName is unknown; typeCall: unresolved")!>b <!OVERLOAD_RESOLUTION_AMBIGUITY!>+=<!> {1}<!>
}

class B {
    private inline operator fun plusAssign(c: () -> C) {}

    private inline operator fun plus(c: () -> C): C = c()
}


// FILE: LibCase1a.kt
package LibPackCase1.a
import testPackCase1.B

inline operator fun B?.plusAssign( c: ()->Int) {} //(1)


// FILE: LibCase1b.kt
package LibPackCase1.b
import LibPackCase1.a.*
import testPackCase1.B

inline operator fun B?.plusAssign( c: ()->C) {} //(2)

class C
