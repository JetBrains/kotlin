// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SPEC VERSION: 0.1-448
 * MAIN LINK: overload-resolution, building-the-overload-candidate-set-ocs, operator-call -> paragraph 1 -> sentence 2
 * PRIMARY LINKS: overload-resolution, choosing-the-most-specific-candidate-from-the-overload-candidate-set, algorithm-of-msc-selection -> paragraph 3 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: plusAssign as inline function
 */

// FILE: TestCase1.kt
/*
 * TESTCASE NUMBER: 1
 */
package testPackCase1

class B {
    inline operator fun plusAssign(crossinline c: ()->C) {}

    inline operator fun plus(crossinline c: ()->C): C = C()
}

@JvmName("bb")
inline operator fun B?.plusAssign( c: ()->Any) { } //(1)
@JvmName("aa")
inline  operator fun B?.plusAssign( c: ()->C) { //(2)

    this <!RECURSION_IN_INLINE!>+=<!> {<!ARGUMENT_TYPE_MISMATCH!>1<!>}
    <!DEBUG_INFO_CALL("fqName: testPackCase1.plusAssign; typeCall: inline operator extension function")!>this <!RECURSION_IN_INLINE!>+=<!> {<!ARGUMENT_TYPE_MISMATCH!>1<!>}<!>
}

class C
